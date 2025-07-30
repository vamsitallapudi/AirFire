import UIKit
import ReplayKit
import VideoToolbox
import Network

class ViewController: UIViewController {
    let recorder = RPScreenRecorder.shared()
    var connection: NWConnection?
    var compressionSession: VTCompressionSession?
    
    @IBOutlet weak var startButton: UIButton!
    let firetvIP = "192.168.1.100" // CHANGE THIS TO YOUR FIRE TV IP
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupConnection()
        setupVideoCompression()
    }
    
    func setupConnection() {
        let host = NWEndpoint.Host(firetvIP)
        let port = NWEndpoint.Port(rawValue: 5000)!
        connection = NWConnection(host: host, port: port, using: .tcp)
        
        connection?.start(queue: .main)
    }
    
    func setupVideoCompression() {
        VTCompressionSessionCreate(
            allocator: nil,
            width: Int32(UIScreen.main.bounds.width * 2),
            height: Int32(UIScreen.main.bounds.height * 2),
            codecType: kCMVideoCodecType_H264,
            encoderSpecification: nil,
            imageBufferAttributes: nil,
            compressedDataAllocator: nil,
            outputCallback: compressionOutputCallback,
            refcon: UnsafeMutableRawPointer(Unmanaged.passUnretained(self).toOpaque()),
            compressionSessionOut: &compressionSession
        )
        
        VTSessionSetProperty(compressionSession!, key: kVTCompressionPropertyKey_RealTime, value: kCFBooleanTrue)
        VTSessionSetProperty(compressionSession!, key: kVTCompressionPropertyKey_ProfileLevel, value: kVTProfileLevel_H264_Baseline_AutoLevel)
        VTSessionSetProperty(compressionSession!, key: kVTCompressionPropertyKey_AverageBitRate, value: 5000000 as CFNumber)
    }
    
    @IBAction func startMirroring() {
        recorder.isMicrophoneEnabled = false
        recorder.startCapture(handler: { (sampleBuffer, bufferType, error) in
            if bufferType == .video {
                self.handleVideoSampleBuffer(sampleBuffer)
            }
        }) { (error) in
            print("Started recording")
        }
    }
    
    func handleVideoSampleBuffer(_ sampleBuffer: CMSampleBuffer) {
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        let presentationTimeStamp = CMSampleBufferGetPresentationTimeStamp(sampleBuffer)
        
        VTCompressionSessionEncodeFrame(
            compressionSession!,
            imageBuffer: pixelBuffer,
            presentationTimeStamp: presentationTimeStamp,
            duration: .invalid,
            frameProperties: nil,
            sourceFrameRefcon: nil,
            infoFlagsOut: nil
        )
    }
    
    func sendData(_ data: Data) {
        // Send length first, then data
        var length = Int32(data.count).bigEndian
        let lengthData = withUnsafeBytes(of: &length) { Data($0) }
        
        connection?.send(content: lengthData, completion: .contentProcessed { _ in
            self.connection?.send(content: data, completion: .contentProcessed { _ in })
        })
    }
}

// Compression callback
func compressionOutputCallback(
    outputCallbackRefCon: UnsafeMutableRawPointer?,
    sourceFrameRefCon: UnsafeMutableRawPointer?,
    status: OSStatus,
    infoFlags: VTEncodeInfoFlags,
    sampleBuffer: CMSampleBuffer?
) {
    guard let sampleBuffer = sampleBuffer else { return }
    let viewController = Unmanaged<ViewController>.fromOpaque(outputCallbackRefCon!).takeUnretainedValue()
    
    // Extract H.264 data
    if let attachments = CMSampleBufferGetSampleAttachmentsArray(sampleBuffer, createIfNecessary: true) {
        let dict = unsafeBitCast(CFArrayGetValueAtIndex(attachments, 0), to: CFMutableDictionary.self)
        CFDictionarySetValue(dict, unsafeBitCast(kCMSampleAttachmentKey_DisplayImmediately, to: UnsafeRawPointer.self), unsafeBitCast(kCFBooleanTrue, to: UnsafeRawPointer.self))
    }
    
    if let dataBuffer = CMSampleBufferGetDataBuffer(sampleBuffer) {
        var length: Int = 0
        var dataPointer: UnsafeMutablePointer<Int8>?
        CMBlockBufferGetDataPointer(dataBuffer, atOffset: 0, lengthAtOffsetOut: nil, totalLengthOut: &length, dataPointerOut: &dataPointer)
        
        if let dataPointer = dataPointer {
            let data = Data(bytes: dataPointer, count: length)
            viewController.sendData(data)
        }
    }
}