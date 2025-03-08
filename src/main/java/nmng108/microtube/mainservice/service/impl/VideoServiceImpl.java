package nmng108.microtube.mainservice.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nmng108.microtube.mainservice.dto.base.BaseResponse;
import nmng108.microtube.mainservice.dto.video.request.CreateVideoDTO;
import nmng108.microtube.mainservice.dto.video.request.UpdateVideoDTO;
import nmng108.microtube.mainservice.dto.video.response.VideoDTO;
import nmng108.microtube.mainservice.entity.Video;
import nmng108.microtube.mainservice.exception.BadRequestException;
import nmng108.microtube.mainservice.repository.ChannelRepository;
import nmng108.microtube.mainservice.repository.VideoRepository;
import nmng108.microtube.mainservice.service.VideoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class VideoServiceImpl implements VideoService {
    VideoRepository videoRepository;
    ChannelRepository channelRepository;

    String processingDir;
    String resultDir;

    public VideoServiceImpl(
            VideoRepository videoRepository, ChannelRepository channelRepository,
            @Value("${video.temp-dir}") String processingDir,
            @Value("${video.result-dir}") String resultDir
    ) {
        this.videoRepository = videoRepository;
        this.channelRepository = channelRepository;
        this.processingDir = processingDir;
        this.resultDir = resultDir;
    }

    @PostConstruct
    public void init() {
        File file = new File(processingDir);

        try {
            Files.createDirectories(Paths.get(resultDir));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!file.exists()) {
            file.mkdir();
            log.info("Folder Created: {}", file.getAbsolutePath());
        } else {
            log.info("Folder already created");
        }

    }

    @Override
    public Mono<BaseResponse<List<VideoDTO>>> getAll() {
        return videoRepository.findAll()
                .map(VideoDTO::new)
                .collectList()
                .map(BaseResponse::succeeded);
    }

    @Override
    public Mono<BaseResponse<VideoDTO>> getById(long id) {
        return videoRepository.findById(id).log()
                .switchIfEmpty(Mono.error(new NoResourceFoundException("")))
                .map(VideoDTO::new)
                .map(BaseResponse::succeeded);
    }

    @Override
    @Transactional
    public Mono<BaseResponse<VideoDTO>> create(CreateVideoDTO dto) {
        return Mono.just(dto.toVideo()).log()
                .filterWhen((v) -> channelRepository.existsById(v.getChannelId()))
                .switchIfEmpty(Mono.error(new BadRequestException("Channel does not exists")))
                .flatMap(videoRepository::save)
                .mapNotNull(VideoDTO::new)
                .mapNotNull(BaseResponse::succeeded);
    }

    @Override
    public Mono<Resource> getMasterFile(long id) {
        return videoRepository.findById(id)
                .switchIfEmpty(Mono.error(new NoResourceFoundException("")))
                .map((v) -> {
                    if (!StringUtils.hasText(v.getDestFilepath())) {
                        throw new BadRequestException("Video is not ready");
                    }

                    Resource resource = new FileSystemResource(STR."\{v.getDestFilepath()}/master.m3u8");

                    return resource;
                })
                ;
    }

    @Override
    public Mono<Resource> getSegmentFile(long id, String resolution, String tsFilename) {
        return videoRepository.findById(id)
                .switchIfEmpty(Mono.error(new NoResourceFoundException("")))
                .map((v) -> {
                    if (!StringUtils.hasText(v.getDestFilepath())) {
                        throw new BadRequestException("Video is not ready");
                    }

                    Resource resource = new FileSystemResource(STR."\{v.getDestFilepath()}/\{resolution}/\{tsFilename}");

                    return resource;
                })
                ;
    }

    @Override
    @Transactional
    public Mono<BaseResponse<Void>> uploadVideo(long id, FilePart file) {
//        String fileExtension = Optional.ofNullable(file.headers().getContentType())
//                .orElseThrow(() -> new InvalidMediaTypeException("", "No \"Content-Type\" provided"))
//                .getSubtype();
        // TODO: add username + user's ID to filename
        Path path = Paths.get(StringUtils.cleanPath(processingDir)).resolve(UUID.randomUUID() + "-" + file.filename());

        return videoRepository.findById(id)
                .switchIfEmpty(Mono.error(new BadRequestException("Video does not exists")))
                .zipWith(file.transferTo(path).thenReturn(path))
                .flatMap((tuple2) -> {
                    Video video = tuple2.getT1();
                    String tempFilepath = path.toFile().getAbsolutePath()/*.replace(":\\", "/")*/.replace("\\", "/");

//                    if (!tempFilepath.startsWith("/")) {
//                        tempFilepath = "/" + tempFilepath;
//                    }

                    video.setOriginalFilename(file.filename());
                    video.setTempFilepath(tempFilepath);

                    return videoRepository.save(video);
                })
                .flatMap((v) -> processVideo(id))
                .thenReturn(BaseResponse.succeeded())
                ;
    }

    private Mono<Void> processVideo(long videoId) {
//        String output360p = HSL_DIR + videoId + "/360p/";
//        String output720p = HSL_DIR + videoId + "/720p/";
//        String output1080p = HSL_DIR + videoId + "/1080p/";

        return videoRepository.findById(videoId)
                .zipWhen((v) -> {
                    Path tmpFilepath = Paths.get(v.getTempFilepath());
                    log.info("tmpFilepath: {}", tmpFilepath);

                    try {
//            Files.createDirectories(Paths.get(output360p));
//            Files.createDirectories(Paths.get(output720p));
//            Files.createDirectories(Paths.get(output1080p));

                        // ffmpeg command
                        Path outputPath = Paths.get(resultDir, tmpFilepath.toFile().getName());

                        Files.createDirectories(outputPath);

                        String outputPathStr = outputPath.toAbsolutePath().toString()/*.replace(":\\", "/")*/.replace("\\", "/");

//                        if (!outputPathStr.startsWith("/")) {
//                            outputPathStr = "/" + outputPathStr;
//                        }
                        log.info("outputPathStr: {}", outputPathStr);
                        // Not work
//                        String ffmpegArgs =
//                                STR." -i \"\{tmpFilepath}\" -c:v libx264 -c:a aac -strict -2 -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename \"\{outputPathStr}/segment_%3d.ts\" -strftime 1 \"\{outputPathStr}/master.m3u8\"";
                        // Works
//                        String ffmpegArgs = STR."""
//                                  -i "\{tmpFilepath}" \
//                                  -codec: copy \
//                                  -hls_time 10 \
//                                  -hls_list_size 0 \
//                                  -f hls \
//                                  \{outputPathStr}/master.m3u8
//                                """;
                        // Not work
//                        String ffmpegArgs = STR."""
//                             -i \{tmpFilepath}
//                            -c:v libx264 -c:a aac
//                            -map 0:v -map 0:a -s:v:0 640x360 -b:v:0 800k
//                            -map 0:v -map 0:a -s:v:1 1280x720 -b:v:1 2800k
//                            -map 0:v -map 0:a -s:v:2 1920x1080 -b:v:2 5000k
//                            -var_stream_map "v:0,a:0 v:1,a:0 v:2,a:0"
//                            -master_pl_name \{outputPathStr}/\{videoId}/master.m3u8
//                            -f hls -hls_time 10 -hls_list_size 0
//                            -hls_segment_filename "\{outputPathStr}/\{videoId}/v%v/fileSequence%d.ts"
//                            "\{outputPathStr}/\{videoId}/v%v/prog_index.m3u8"
//                            """;
//                        STR."""
//                             ffmpeg -i \{tmpFilepath}
//                              -filter_complex
//                               [0:v]split=3[v1][v2][v3];\\s[v1]scale=w=1920:h=1080[v1out];\\s[v2]scale=w=1280:h=720[v2out];\\s[v3]scale=w=854:h=480[v3out]
//                              -map [v1out] -c:v:0 libx264 -b:v:0 5000k -maxrate:v:0 5350k -bufsize:v:0 7500k
//                              -map [v2out] -c:v:1 libx264 -b:v:1 2800k -maxrate:v:1 2996k -bufsize:v:1 4200k
//                              -map [v3out] -c:v:2 libx264 -b:v:2 1400k -maxrate:v:2 1498k -bufsize:v:2 2100k
//                              -map a:0 -c:a aac -b:a:0 192k -ac 2
//                              -map a:0 -c:a aac -b:a:1 128k -ac 2
//                              -map a:0 -c:a aac -b:a:2 96k -ac 2
//                              -f hls
//                              -hls_time 10
//                              -hls_playlist_type vod
//                              -hls_flags independent_segments
//                              -hls_segment_type mpegts
//                              -hls_segment_filename \{outputPathStr}/stream_%v/data%03d.ts
//                              -master_pl_name master.m3u8
//                              -var_stream_map v:0,a:0\\sv:1,a:1\\sv:2,a:2
//                              \{outputPathStr}/stream_%v/playlist.m3u8
//                             """

                        String[] ffmpegArgs = {
                                "ffmpeg", "-i", STR."\{tmpFilepath}",
                                "-filter_complex", "[0:v]split=3[v1][v2][v3]; [v1]scale=w=1920:h=1080[v1out]; [v2]scale=w=1280:h=720[v2out]; [v3]scale=w=854:h=480[v3out]",
                                "-map", "[v1out]", "-c:v:0", "libx264", "-b:v:0", "5000k", "-maxrate:v:0", "5350k", "-bufsize:v:0", "7500k",
                                "-map", "[v2out]", "-c:v:1", "libx264", "-b:v:1", "2800k", "-maxrate:v:1", "2996k", "-bufsize:v:1", "4200k",
                                "-map", "[v3out]", "-c:v:2", "libx264", "-b:v:2", "1400k", "-maxrate:v:2", "1498k", "-bufsize:v:2", "2100k",
                                "-map", "a:0", "-c:a", "aac", "-b:a:0", "192k", "-ac", "2",
                                "-map", "a:0", "-c:a", "aac", "-b:a:1", "128k", "-ac", "2",
                                "-map", "a:0", "-c:a", "aac", "-b:a:2", "96k", "-ac", "2",
                                "-f", "hls",
                                "-hls_time", "10",
                                "-hls_playlist_type", "vod",
                                "-hls_flags", "independent_segments",
                                "-hls_segment_type", "mpegts",
                                "-hls_segment_filename", STR."\{outputPathStr}/stream_%v/data%03d.ts",
                                "-master_pl_name", "master.m3u8",
                                "-var_stream_map", "v:0,a:0 v:1,a:1 v:2,a:2",
                                STR."\{outputPathStr}/stream_%v/playlist.m3u8"
                        };

                        log.info(String.join(",", ffmpegArgs));
                        //file this command
                        ProcessBuilder processBuilder = new ProcessBuilder(ffmpegArgs
//                                Arrays.stream(ffmpegArgs.split("[ \n]+"))
//                                        .map((s) -> s.replaceAll("\\\\s", " "))
//                                        .toArray(String[]::new)
                        );

                        processBuilder.inheritIO();

                        Process process = processBuilder.start();
                        int exit = process.waitFor();
//                        log.info(new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));

                        if (exit != 0) {
//                            log.info(new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
                            throw new RuntimeException("video processing failed!!");
                        }

                        return Mono.just(outputPath);
                    } catch (IOException ex) {
                        throw new RuntimeException("Video processing fail!! " + ex.getMessage());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                })
                .publishOn(Schedulers.boundedElastic())
                .map((tuple2) -> {
                    Path outputPath = tuple2.getT2();
                    File originalMasterFile = outputPath.resolve("master.m3u8").toFile();
                    File destMasterFile = outputPath.resolve("dest-master.m3u8").toFile();

                    // Rename folders "stream_%v" with corresponding resolution numbers.
                    // Destination folder names & number of streams are temporarily fixed
                    try (
                            BufferedReader originalMasterFileReader = new BufferedReader(new FileReader(originalMasterFile));
                            FileWriter destMasterFileWriter = new FileWriter(destMasterFile);
                    ) {
                        String line;

                        // Rename folders in master file
                        while ((line = originalMasterFileReader.readLine()) != null) {
                            line = line.replaceAll("^stream_" + 0, "1080p")
                                    .replaceAll("^stream_" + 1, "720p")
                                    .replaceAll("^stream_" + 2, "480p");
                            destMasterFileWriter.write(line + "\r\n");
                        }

                        // Rename actual folders
                        outputPath.resolve("stream_0").toFile().renameTo(outputPath.resolve("1080p").toFile());
                        outputPath.resolve("stream_1").toFile().renameTo(outputPath.resolve("720p").toFile());
                        outputPath.resolve("stream_2").toFile().renameTo(outputPath.resolve("480p").toFile());
                    } catch (IOException ex) {
                        destMasterFile.delete();

                        throw new RuntimeException("Video processing fail! Reason: " + ex.getMessage());
                    }

                    if (!(originalMasterFile.delete() && destMasterFile.renameTo(originalMasterFile))) {
                        throw new RuntimeException("Could not rename folders");
                    }

                    return tuple2;
                })
                .flatMap((tuple2) -> {
                    var video = tuple2.getT1();
                    String destFilepath = tuple2.getT2().toFile().getAbsolutePath();

                    try {
                        Files.delete(Paths.get(video.getTempFilepath()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    video.setTempFilepath(null);
                    video.setDestFilepath(destFilepath);

                    return videoRepository.save(video);
                })
                .then();
    }

    @Override
    public Mono<BaseResponse<VideoDTO>> updateInfo(long id, UpdateVideoDTO dto) {
        return videoRepository.findById(id).log()
                .flatMap((v) -> {
                    Optional.ofNullable(dto.getName()).ifPresent(v::setName);
                    Optional.ofNullable(dto.getDescription()).ifPresent(v::setDescription);
                    Optional.ofNullable(dto.getVisibility()).ifPresent(v::setVisibility);

                    return videoRepository.save(v);
                })
                .mapNotNull(VideoDTO::new)
                .mapNotNull(BaseResponse::succeeded);
//                .(() -> new BadRequestException("Video not found"));
    }

    @Override
    public Mono<BaseResponse<Void>> delete(long id) {
        return videoRepository.findById(id).log()
                .flatMap(videoRepository::delete)
                .then(Mono.fromCallable(BaseResponse::succeeded));
    }
}
