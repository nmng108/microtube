package nmng108.microtube.mainservice.service;

import nmng108.microtube.mainservice.dto.base.BaseResponse;
import nmng108.microtube.mainservice.dto.video.request.CreateVideoDTO;
import nmng108.microtube.mainservice.dto.video.request.UpdateVideoDTO;
import nmng108.microtube.mainservice.dto.video.response.VideoDTO;
import org.springframework.core.io.Resource;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.List;

public interface VideoService {
    Mono<BaseResponse<List<VideoDTO>>> getAll();

    Mono<BaseResponse<VideoDTO>> getById(long id);

    Mono<BaseResponse<VideoDTO>> create(CreateVideoDTO video);

    /**
     * Fetch master file
     */
    Mono<Resource> getMasterFile(long id);
    Mono<Resource> getSegmentFile(long id, String resolution, String tsFilename);

    Mono<BaseResponse<Void>> uploadVideo(long id, FilePart file);

    Mono<BaseResponse<VideoDTO>> updateInfo(long id, UpdateVideoDTO video);

    Mono<BaseResponse<Void>> delete(long id);
}