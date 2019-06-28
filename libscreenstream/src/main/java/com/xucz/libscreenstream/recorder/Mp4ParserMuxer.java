package com.xucz.libscreenstream.recorder;

import android.media.MediaCodec;
import android.media.MediaFormat;

import com.coremedia.iso.BoxParser;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.boxes.AbstractMediaHeaderBox;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.DataEntryUrlBox;
import com.coremedia.iso.boxes.DataInformationBox;
import com.coremedia.iso.boxes.DataReferenceBox;
import com.coremedia.iso.boxes.FileTypeBox;
import com.coremedia.iso.boxes.HandlerBox;
import com.coremedia.iso.boxes.MediaBox;
import com.coremedia.iso.boxes.MediaHeaderBox;
import com.coremedia.iso.boxes.MediaInformationBox;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.MovieHeaderBox;
import com.coremedia.iso.boxes.SampleDescriptionBox;
import com.coremedia.iso.boxes.SampleSizeBox;
import com.coremedia.iso.boxes.SampleTableBox;
import com.coremedia.iso.boxes.SampleToChunkBox;
import com.coremedia.iso.boxes.SoundMediaHeaderBox;
import com.coremedia.iso.boxes.StaticChunkOffsetBox;
import com.coremedia.iso.boxes.SyncSampleBox;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.coremedia.iso.boxes.TimeToSampleBox.Entry;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.coremedia.iso.boxes.VideoMediaHeaderBox;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.coremedia.iso.boxes.sampleentry.VisualSampleEntry;
import com.googlecode.mp4parser.DataSource;
import com.googlecode.mp4parser.boxes.mp4.ESDescriptorBox;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.AudioSpecificConfig;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.DecoderConfigDescriptor;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.ESDescriptor;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.SLConfigDescriptor;
import com.googlecode.mp4parser.util.Math;
import com.googlecode.mp4parser.util.Matrix;
import com.mp4parser.iso14496.part15.AvcConfigurationBox;
import com.xucz.libscreenstream.log.PushLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-26
 */
public class Mp4ParserMuxer extends MultiMp4Muxer {
    private static final String TAG = Mp4ParserMuxer.class.getSimpleName() + " dq";
    private static Map<Integer, Integer> samplingFrequencyIndexMap = new HashMap();
    public static final String VCODEC_NAME = "video/avc";
    private Mp4ParserMuxer.Mp4Movie mp4Movie = null;
    private Mp4ParserMuxer.InterleaveChunkMdat mdat = null;
    private FileOutputStream fos = null;
    private FileChannel fc = null;
    private volatile long recFileSize = 0L;
    private volatile long mdatOffset = 0L;
    private volatile long flushBytes = 0L;
    private HashMap<Mp4ParserMuxer.Track, long[]> track2SampleSizes = new HashMap();

    public Mp4ParserMuxer(RecordHandler recordHandler, RecordConfig recordConfig) {
        super(recordHandler, recordConfig);
    }

    public synchronized boolean initMuxer() {
        try {
            this.mp4Movie = new Mp4ParserMuxer.Mp4Movie();
            this.createMovie(this.mRecFile);
            this.mp4Movie.addTrack(this.videoFormat, false);
            if (this.isWriteAudioData) {
                this.mp4Movie.addTrack(this.audioFormat, true);
            }

            return true;
        } catch (Exception var2) {
            PushLog.d(TAG, "initMuxer=" + var2);
            return false;
        }
    }

    public synchronized void muxerFrame2Mp4(SrsEsFrame frame) {
        if (frame != null) {
            this.writeSampleData2Mp4(frame.bb, frame.bi, frame.is_audio());
        }

    }

    protected synchronized void stopMuxer() {
        this.videoKeyframeCheckNum = 0;
        this.needToFindKeyFrame = true;
        this.aacSpecConfig = false;
        if (this.mp4Movie != null) {
            this.finishMovie();
        }

    }

    private synchronized void writeSampleData2Mp4(ByteBuffer byteBuf, MediaCodec.BufferInfo bi, boolean isAudio) {
        int trackIndex = isAudio ? 11 : 10;
        if (byteBuf != null && bi != null && bi.size != 0 && this.mp4Movie.getTracks().containsKey(trackIndex)) {
            try {
                if (this.mdat.first) {
                    this.mdat.setContentSize(0L);
                    this.mdat.getBox(this.fc);
                    this.mdatOffset = this.recFileSize;
                    this.recFileSize += (long) this.mdat.getHeaderSize();
                    this.mdat.first = false;
                }

                int bufferCapacity = byteBuf.capacity();
                int limit = byteBuf.limit();
                int newPosition = bi.offset;
                if (this.isOffset && !isAudio) {
                    newPosition = bi.offset + 4;
                }

                int newLimit = bi.offset + bi.size;
                if (newLimit > bufferCapacity || newLimit < 0 || newPosition > newLimit) {
                    String msg = "Bad position:newLimit=" + newLimit + ",bufferLimit=" + limit + ",bufferCapacity=" + bufferCapacity;
                    PushLog.d(TAG, msg);
                    return;
                }

                boolean isError = false;
                int writeBytes = 0;

                try {
                    this.mp4Movie.addSample(trackIndex, this.recFileSize, bi);
                    byteBuf.position(newPosition);
                    byteBuf.limit(newLimit);
                    if (!isAudio) {
                        ByteBuffer size = ByteBuffer.allocateDirect(4);
                        size.position(0);
                        size.putInt(bi.size - 4);
                        size.position(0);
                        this.recFileSize += (long) this.fc.write(size);
                    }

                    writeBytes = this.fc.write(byteBuf);
                } catch (Exception var12) {
                    isError = true;
                    PushLog.d(TAG, "duqian Bad position?=" + var12 + ", bi.offset=" + bi.offset + ",bi.size=" + bi.size);
                    if (this.mHandler != null && this.isRecordByKeyFrame) {
                        this.mHandler.notifyRecordException(var12, this.filePath);
                    }
                }

                if (!isError) {
                    this.recFileSize += (long) writeBytes;
                    this.flushBytes += (long) writeBytes;
                    if (this.flushBytes > 65536L) {
                        this.fos.flush();
                        this.flushBytes = 0L;
                    }
                }
            } catch (Exception var13) {
                if (this.mHandler != null) {
                    this.mHandler.notifyRecordException(var13, this.filePath);
                }

                PushLog.d("dq writeSampleData2Mp4 error " + var13);
            }

        }
    }

    private void createMovie(File outputFile) {
        try {
            this.fos = new FileOutputStream(outputFile);
            this.fc = this.fos.getChannel();
            this.mdat = new Mp4ParserMuxer.InterleaveChunkMdat();
            this.mdatOffset = 0L;
            FileTypeBox fileTypeBox = this.createFileTypeBox();
            fileTypeBox.getBox(this.fc);
            this.recFileSize += fileTypeBox.getSize();
        } catch (IOException var3) {
            if (this.mHandler != null) {
                this.mHandler.notifyRecordException(var3, this.filePath);
            }

            PushLog.d(TAG, "create movie error " + var3);
        }

    }

    private synchronized void finishMovie() {
        try {
            if (this.flushBytes > 0L) {
                this.fos.flush();
                this.flushBytes = 0L;
            }

            if (this.mdat != null && this.mdat.getSize() != 0L && this.fc != null) {
                long oldPosition = this.fc.position();
                this.fc.position(this.mdatOffset);
                this.mdat.setContentSize(this.recFileSize - (long) this.mdat.getHeaderSize() - this.mdatOffset);
                this.mdat.getBox(this.fc);
                this.fc.position(oldPosition);
                this.mdat.setContentSize(0L);
                this.fos.flush();
            }

            Iterator var7 = this.mp4Movie.getTracks().values().iterator();

            while (var7.hasNext()) {
                Mp4ParserMuxer.Track track = (Mp4ParserMuxer.Track) var7.next();
                List<Sample> samples = track.getSamples();
                long[] sizes = new long[samples.size()];

                for (int i = 0; i < sizes.length; ++i) {
                    sizes[i] = ((Mp4ParserMuxer.Sample) samples.get(i)).getSize();
                }

                this.track2SampleSizes.put(track, sizes);
            }

            Box moov = this.createMovieBox(this.mp4Movie);
            if (this.fc != null) {
                moov.getBox(this.fc);
            }

            if (this.fos != null) {
                this.fos.flush();
            }

            if (this.fc != null) {
                this.fc.close();
            }

            if (this.fos != null) {
                this.fos.close();
            }

            this.mp4Movie.getTracks().clear();
            this.track2SampleSizes.clear();
            this.recFileSize = 0L;
            this.flushBytes = 0L;
        } catch (Exception var6) {
            PushLog.d(TAG, "finishMovie error " + var6);
            if (this.mHandler != null) {
                this.mHandler.notifyRecordException(var6, this.filePath);
            }
        }

    }

    private FileTypeBox createFileTypeBox() {
        LinkedList<String> minorBrands = new LinkedList();
        minorBrands.add("isom");
        minorBrands.add("3gp4");
        return new FileTypeBox("isom", 0L, minorBrands);
    }

    private long getTimescale(Mp4ParserMuxer.Mp4Movie mp4Movie) {
        long timescale = 0L;
        if (!mp4Movie.getTracks().isEmpty()) {
            timescale = (long) ((Mp4ParserMuxer.Track) mp4Movie.getTracks().values().iterator().next()).getTimeScale();
        }

        Mp4ParserMuxer.Track track;
        for (Iterator var4 = mp4Movie.getTracks().values().iterator(); var4.hasNext(); timescale = Math.gcd((long) track.getTimeScale(), timescale)) {
            track = (Mp4ParserMuxer.Track) var4.next();
        }

        return timescale;
    }

    private MovieBox createMovieBox(Mp4ParserMuxer.Mp4Movie movie) {
        MovieBox movieBox = new MovieBox();
        MovieHeaderBox mvhd = new MovieHeaderBox();
        mvhd.setCreationTime(new Date());
        mvhd.setModificationTime(new Date());
        mvhd.setMatrix(Matrix.ROTATE_0);
        long movieTimeScale = this.getTimescale(movie);
        long duration = 0L;
        Iterator var8 = movie.getTracks().values().iterator();

        Mp4ParserMuxer.Track track;
        while (var8.hasNext()) {
            track = (Mp4ParserMuxer.Track) var8.next();
            long tracksDuration = track.getDuration() * movieTimeScale / (long) track.getTimeScale();
            if (tracksDuration > duration) {
                duration = tracksDuration;
            }
        }

        mvhd.setDuration(duration);
        mvhd.setTimescale(movieTimeScale);
        mvhd.setNextTrackId((long) (movie.getTracks().size() + 1));
        movieBox.addBox(mvhd);
        var8 = movie.getTracks().values().iterator();

        while (var8.hasNext()) {
            track = (Mp4ParserMuxer.Track) var8.next();
            movieBox.addBox(this.createTrackBox(track, movie));
        }

        return movieBox;
    }

    private TrackBox createTrackBox(Mp4ParserMuxer.Track track, Mp4ParserMuxer.Mp4Movie movie) {
        TrackBox trackBox = new TrackBox();
        TrackHeaderBox tkhd = new TrackHeaderBox();
        tkhd.setEnabled(true);
        tkhd.setInMovie(true);
        tkhd.setInPreview(true);
        if (track.isAudio()) {
            tkhd.setMatrix(Matrix.ROTATE_0);
        } else {
            tkhd.setMatrix(movie.getMatrix());
        }

        tkhd.setAlternateGroup(0);
        tkhd.setCreationTime(track.getCreationTime());
        tkhd.setModificationTime(track.getCreationTime());
        tkhd.setDuration(track.getDuration() * this.getTimescale(movie) / (long) track.getTimeScale());
        tkhd.setHeight((double) track.getHeight());
        tkhd.setWidth((double) track.getWidth());
        tkhd.setLayer(0);
        tkhd.setModificationTime(new Date());
        tkhd.setTrackId((long) (track.getTrackId() + 1));
        tkhd.setVolume(track.getVolume());
        trackBox.addBox(tkhd);
        MediaBox mdia = new MediaBox();
        trackBox.addBox(mdia);
        MediaHeaderBox mdhd = new MediaHeaderBox();
        mdhd.setCreationTime(track.getCreationTime());
        mdhd.setModificationTime(track.getCreationTime());
        mdhd.setDuration(track.getDuration());
        mdhd.setTimescale((long) track.getTimeScale());
        mdhd.setLanguage("eng");
        mdia.addBox(mdhd);
        HandlerBox hdlr = new HandlerBox();
        hdlr.setName(track.isAudio() ? "SoundHandle" : "VideoHandle");
        hdlr.setHandlerType(track.getHandler());
        mdia.addBox(hdlr);
        MediaInformationBox minf = new MediaInformationBox();
        minf.addBox(track.getMediaHeaderBox());
        DataInformationBox dinf = new DataInformationBox();
        DataReferenceBox dref = new DataReferenceBox();
        dinf.addBox(dref);
        DataEntryUrlBox url = new DataEntryUrlBox();
        url.setFlags(1);
        dref.addBox(url);
        minf.addBox(dinf);
        Box stbl = this.createStbl(track);
        minf.addBox(stbl);
        mdia.addBox(minf);
        return trackBox;
    }

    private Box createStbl(Mp4ParserMuxer.Track track) {
        SampleTableBox stbl = new SampleTableBox();
        this.createStsd(track, stbl);
        this.createStts(track, stbl);
        this.createStss(track, stbl);
        this.createStsc(track, stbl);
        this.createStsz(track, stbl);
        this.createStco(track, stbl);
        return stbl;
    }

    private void createStsd(Mp4ParserMuxer.Track track, SampleTableBox stbl) {
        stbl.addBox(track.getSampleDescriptionBox());
    }

    private void createStts(Mp4ParserMuxer.Track track, SampleTableBox stbl) {
        Entry lastEntry = null;
        List<Entry> entries = new ArrayList();
        Iterator var5 = track.getSampleDurations().iterator();

        while (true) {
            while (var5.hasNext()) {
                long delta = (Long) var5.next();
                if (lastEntry != null && lastEntry.getDelta() == delta) {
                    lastEntry.setCount(lastEntry.getCount() + 1L);
                } else {
                    lastEntry = new Entry(1L, delta);
                    entries.add(lastEntry);
                }
            }

            TimeToSampleBox stts = new TimeToSampleBox();
            stts.setEntries(entries);
            stbl.addBox(stts);
            return;
        }
    }

    private void createStss(Mp4ParserMuxer.Track track, SampleTableBox stbl) {
        long[] syncSamples = track.getSyncSamples();
        if (syncSamples != null && syncSamples.length > 0) {
            SyncSampleBox stss = new SyncSampleBox();
            stss.setSampleNumber(syncSamples);
            stbl.addBox(stss);
        }

    }

    private void createStsc(Mp4ParserMuxer.Track track, SampleTableBox stbl) {
        SampleToChunkBox stsc = new SampleToChunkBox();
        stsc.setEntries(new LinkedList());
        int lastChunkNumber = 1;
        int lastSampleCount = 0;
        int previousWritedChunkCount = -1;
        int samplesCount = track.getSamples().size();

        for (int a = 0; a < samplesCount; ++a) {
            Mp4ParserMuxer.Sample sample = (Mp4ParserMuxer.Sample) track.getSamples().get(a);
            long offset = sample.getOffset();
            long size = sample.getSize();
            long lastOffset = offset + size;
            ++lastSampleCount;
            boolean write = false;
            if (a != samplesCount - 1) {
                Mp4ParserMuxer.Sample nextSample = (Mp4ParserMuxer.Sample) track.getSamples().get(a + 1);
                if (lastOffset != nextSample.getOffset()) {
                    write = true;
                }
            } else {
                write = true;
            }

            if (write) {
                if (previousWritedChunkCount != lastSampleCount) {
                    stsc.getEntries().add(new com.coremedia.iso.boxes.SampleToChunkBox.Entry((long) lastChunkNumber, (long) lastSampleCount, 1L));
                    previousWritedChunkCount = lastSampleCount;
                }

                lastSampleCount = 0;
                ++lastChunkNumber;
            }
        }

        stbl.addBox(stsc);
    }

    private void createStsz(Mp4ParserMuxer.Track track, SampleTableBox stbl) {
        SampleSizeBox stsz = new SampleSizeBox();
        stsz.setSampleSizes((long[]) this.track2SampleSizes.get(track));
        stbl.addBox(stsz);
    }

    private void createStco(Mp4ParserMuxer.Track track, SampleTableBox stbl) {
        ArrayList<Long> chunksOffsets = new ArrayList();
        long lastOffset = -1L;

        Mp4ParserMuxer.Sample sample;
        long offset;
        for (Iterator var6 = track.getSamples().iterator(); var6.hasNext(); lastOffset = offset + sample.getSize()) {
            sample = (Mp4ParserMuxer.Sample) var6.next();
            offset = sample.getOffset();
            if (lastOffset != -1L && lastOffset != offset) {
                lastOffset = -1L;
            }

            if (lastOffset == -1L) {
                chunksOffsets.add(offset);
            }
        }

        long[] chunkOffsetsLong = new long[chunksOffsets.size()];

        for (int a = 0; a < chunksOffsets.size(); ++a) {
            chunkOffsetsLong[a] = (Long) chunksOffsets.get(a);
        }

        StaticChunkOffsetBox stco = new StaticChunkOffsetBox();
        stco.setChunkOffsets(chunkOffsetsLong);
        stbl.addBox(stco);
    }

    static {
        samplingFrequencyIndexMap.put(96000, 0);
        samplingFrequencyIndexMap.put(88200, 1);
        samplingFrequencyIndexMap.put(56000, 2);
        samplingFrequencyIndexMap.put(48000, 3);
        samplingFrequencyIndexMap.put(40000, 4);
        samplingFrequencyIndexMap.put(32000, 5);
        samplingFrequencyIndexMap.put(24000, 6);
        samplingFrequencyIndexMap.put(22050, 7);
        samplingFrequencyIndexMap.put(16000, 8);
        samplingFrequencyIndexMap.put(12000, 9);
        samplingFrequencyIndexMap.put(11025, 10);
        samplingFrequencyIndexMap.put(8000, 11);
    }

    private class Track {
        private int trackId = 0;
        private ArrayList<Mp4ParserMuxer.Sample> samples = new ArrayList();
        private long duration = 0L;
        private String handler;
        private AbstractMediaHeaderBox headerBox = null;
        private SampleDescriptionBox sampleDescriptionBox = null;
        private LinkedList<Integer> syncSamples = null;
        private int timeScale;
        private Date creationTime = new Date();
        private int height;
        private int width;
        private float volume = 0.0F;
        private ArrayList<Long> sampleDurations = new ArrayList();
        private boolean isAudio = false;
        private long lastPresentationTimeUs = 0L;
        private boolean first = true;

        public Track(int id, MediaFormat format, boolean audio) {
            this.trackId = id;
            this.isAudio = audio;
            if (!this.isAudio) {
                this.sampleDurations.add(3015L);
                this.duration = 3015L;
                this.width = format.getInteger("width");
                this.height = format.getInteger("height");
                this.timeScale = 90000;
                this.syncSamples = new LinkedList();
                this.handler = "vide";
                this.headerBox = new VideoMediaHeaderBox();
                this.sampleDescriptionBox = new SampleDescriptionBox();
                if (format.getString("mime").contentEquals("video/avc")) {
                    VisualSampleEntry visualSampleEntry = new VisualSampleEntry("avc1");
                    visualSampleEntry.setDataReferenceIndex(1);
                    visualSampleEntry.setDepth(24);
                    visualSampleEntry.setFrameCount(1);
                    visualSampleEntry.setHorizresolution(72.0D);
                    visualSampleEntry.setVertresolution(72.0D);
                    visualSampleEntry.setWidth(this.width);
                    visualSampleEntry.setHeight(this.height);
                    visualSampleEntry.setCompressorname("AVC Coding");
                    AvcConfigurationBox avcConfigurationBox = new AvcConfigurationBox();
                    avcConfigurationBox.setConfigurationVersion(1);
                    avcConfigurationBox.setAvcProfileIndication(Mp4ParserMuxer.this.h264_sps.get(1));
                    avcConfigurationBox.setProfileCompatibility(0);
                    avcConfigurationBox.setAvcLevelIndication(Mp4ParserMuxer.this.h264_sps.get(3));
                    avcConfigurationBox.setLengthSizeMinusOne(3);
                    avcConfigurationBox.setSequenceParameterSets(Mp4ParserMuxer.this.spsList);
                    avcConfigurationBox.setPictureParameterSets(Mp4ParserMuxer.this.ppsList);
                    avcConfigurationBox.setBitDepthLumaMinus8(-1);
                    avcConfigurationBox.setBitDepthChromaMinus8(-1);
                    avcConfigurationBox.setChromaFormat(-1);
                    avcConfigurationBox.setHasExts(false);
                    visualSampleEntry.addBox(avcConfigurationBox);
                    this.sampleDescriptionBox.addBox(visualSampleEntry);
                }
            } else {
                this.sampleDurations.add(1024L);
                this.duration = 1024L;
                this.volume = 1.0F;
                this.timeScale = format.getInteger("sample-rate");
                this.handler = "soun";
                this.headerBox = new SoundMediaHeaderBox();
                this.sampleDescriptionBox = new SampleDescriptionBox();
                AudioSampleEntry audioSampleEntry = new AudioSampleEntry("mp4a");
                audioSampleEntry.setChannelCount(format.getInteger("channel-count"));
                audioSampleEntry.setSampleRate((long) format.getInteger("sample-rate"));
                audioSampleEntry.setDataReferenceIndex(1);
                audioSampleEntry.setSampleSize(16);
                ESDescriptorBox esds = new ESDescriptorBox();
                ESDescriptor descriptor = new ESDescriptor();
                descriptor.setEsId(0);
                SLConfigDescriptor slConfigDescriptor = new SLConfigDescriptor();
                slConfigDescriptor.setPredefined(2);
                descriptor.setSlConfigDescriptor(slConfigDescriptor);
                DecoderConfigDescriptor decoderConfigDescriptor = new DecoderConfigDescriptor();
                decoderConfigDescriptor.setObjectTypeIndication(64);
                decoderConfigDescriptor.setStreamType(5);
                decoderConfigDescriptor.setBufferSizeDB(1536);
                decoderConfigDescriptor.setMaxBitRate(96000L);
                decoderConfigDescriptor.setAvgBitRate(96000L);
                AudioSpecificConfig audioSpecificConfig = new AudioSpecificConfig();
                audioSpecificConfig.setAudioObjectType(2);
                audioSpecificConfig.setSamplingFrequencyIndex((Integer) Mp4ParserMuxer.samplingFrequencyIndexMap.get((int) audioSampleEntry.getSampleRate()));
                audioSpecificConfig.setChannelConfiguration(audioSampleEntry.getChannelCount());
                decoderConfigDescriptor.setAudioSpecificInfo(audioSpecificConfig);
                descriptor.setDecoderConfigDescriptor(decoderConfigDescriptor);
                ByteBuffer data = descriptor.serialize();
                esds.setEsDescriptor(descriptor);
                esds.setData(data);
                audioSampleEntry.addBox(esds);
                this.sampleDescriptionBox.addBox(audioSampleEntry);
            }

        }

        public void addSample(long offset, MediaCodec.BufferInfo bi) {
            long delta = bi.presentationTimeUs - this.lastPresentationTimeUs;
            if (delta >= 0L) {
                boolean isSyncFrame = !this.isAudio && (bi.flags & 1) != 0;
                this.samples.add(Mp4ParserMuxer.this.new Sample(offset, (long) bi.size));
                if (this.syncSamples != null && isSyncFrame) {
                    this.syncSamples.add(this.samples.size());
                }

                delta = (delta * (long) this.timeScale + 500000L) / 1000000L;
                this.lastPresentationTimeUs = bi.presentationTimeUs;
                if (!this.first) {
                    this.sampleDurations.add(this.sampleDurations.size() - 1, delta);
                    this.duration += delta;
                }

                this.first = false;
            }
        }

        public void clearSample() {
            this.first = true;
            this.samples.clear();
            this.syncSamples.clear();
            this.sampleDurations.clear();
        }

        public ArrayList<Mp4ParserMuxer.Sample> getSamples() {
            return this.samples;
        }

        public long getDuration() {
            return this.duration;
        }

        public String getHandler() {
            return this.handler;
        }

        public AbstractMediaHeaderBox getMediaHeaderBox() {
            return this.headerBox;
        }

        public SampleDescriptionBox getSampleDescriptionBox() {
            return this.sampleDescriptionBox;
        }

        public long[] getSyncSamples() {
            if (this.syncSamples != null && !this.syncSamples.isEmpty()) {
                long[] returns = new long[this.syncSamples.size()];

                for (int i = 0; i < this.syncSamples.size(); ++i) {
                    returns[i] = (long) (Integer) this.syncSamples.get(i);
                }

                return returns;
            } else {
                return null;
            }
        }

        public int getTimeScale() {
            return this.timeScale;
        }

        public Date getCreationTime() {
            return this.creationTime;
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }

        public float getVolume() {
            return this.volume;
        }

        public ArrayList<Long> getSampleDurations() {
            return this.sampleDurations;
        }

        public boolean isAudio() {
            return this.isAudio;
        }

        public int getTrackId() {
            return this.trackId;
        }
    }

    private class Sample {
        private long offset = 0L;
        private long size = 0L;

        public Sample(long offset, long size) {
            this.offset = offset;
            this.size = size;
        }

        public long getOffset() {
            return this.offset;
        }

        public long getSize() {
            return this.size;
        }
    }

    private class InterleaveChunkMdat implements Box {
        private boolean first;
        private Container parent;
        private ByteBuffer header;
        private long contentSize;

        private InterleaveChunkMdat() {
            this.first = true;
            this.header = ByteBuffer.allocateDirect(16);
            this.contentSize = 1073741824L;
        }

        public Container getParent() {
            return this.parent;
        }

        public void setParent(Container parent) {
            this.parent = parent;
        }

        public void setContentSize(long contentSize) {
            this.contentSize = contentSize;
        }

        public long getContentSize() {
            return this.contentSize;
        }

        public String getType() {
            return "mdat";
        }

        public long getSize() {
            return (long) this.header.limit() + this.contentSize;
        }

        @Override
        public long getOffset() {
            return 0;
        }

        public int getHeaderSize() {
            return this.header.limit();
        }

        private boolean isSmallBox(long contentSize) {
            return contentSize + (long) this.header.limit() < 4294967296L;
        }

        public void getBox(WritableByteChannel writableByteChannel) {
            this.header.rewind();
            long size = this.getSize();
            if (this.isSmallBox(size)) {
                IsoTypeWriter.writeUInt32(this.header, size);
            } else {
                IsoTypeWriter.writeUInt32(this.header, 1L);
            }

            this.header.put(IsoFile.fourCCtoBytes("mdat"));
            if (this.isSmallBox(size)) {
                this.header.put(new byte[8]);
            } else {
                IsoTypeWriter.writeUInt64(this.header, size);
            }

            this.header.rewind();

            try {
                writableByteChannel.write(this.header);
            } catch (IOException var5) {
                PushLog.d("dq writableByteChannel error " + var5);
                if (Mp4ParserMuxer.this.mHandler != null) {
                    Mp4ParserMuxer.this.mHandler.notifyRecordException(var5, Mp4ParserMuxer.this.filePath);
                }
            }

        }

        @Override
        public void parse(DataSource dataSource, ByteBuffer header, long contentSize, BoxParser boxParser) throws IOException {

        }
    }

    private class Mp4Movie {
        private Matrix matrix;
        private HashMap<Integer, Mp4ParserMuxer.Track> tracks;

        public Mp4Movie() {
            this.matrix = Matrix.ROTATE_0;
            this.tracks = new HashMap();
            this.updateMatrix();
        }

        public void updateMatrix() {
            if (Mp4ParserMuxer.this.mOrientation == 90) {
                this.matrix = Matrix.ROTATE_90;
            } else if (Mp4ParserMuxer.this.mOrientation == 180) {
                this.matrix = Matrix.ROTATE_180;
            } else if (Mp4ParserMuxer.this.mOrientation == 270) {
                this.matrix = Matrix.ROTATE_270;
            } else {
                this.matrix = Matrix.ROTATE_0;
            }

        }

        public Matrix getMatrix() {
            return this.matrix;
        }

        public HashMap<Integer, Mp4ParserMuxer.Track> getTracks() {
            return this.tracks;
        }

        public void addSample(int trackIndex, long offset, MediaCodec.BufferInfo bi) {
            Mp4ParserMuxer.Track track = (Mp4ParserMuxer.Track) this.tracks.get(trackIndex);
            track.addSample(offset, bi);
        }

        public void addTrack(MediaFormat format, boolean isAudio) {
            if (format != null) {
                if (isAudio) {
                    this.tracks.put(11, Mp4ParserMuxer.this.new Track(this.tracks.size(), format, true));
                } else {
                    this.tracks.put(10, Mp4ParserMuxer.this.new Track(this.tracks.size(), format, false));
                }
            }

        }

        public void removeTrack(int trackIndex) {
            this.tracks.remove(trackIndex);
        }
    }
}
