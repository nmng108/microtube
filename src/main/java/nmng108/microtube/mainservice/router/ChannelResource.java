package nmng108.microtube.mainservice.router;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nmng108.microtube.mainservice.dto.base.BaseResponse;
import nmng108.microtube.mainservice.dto.channel.request.CreateChannelDTO;
import nmng108.microtube.mainservice.dto.channel.request.UpdateChannelDTO;
import nmng108.microtube.mainservice.dto.channel.response.ChannelDTO;
import nmng108.microtube.mainservice.service.ChannelService;
import nmng108.microtube.mainservice.util.constant.Routes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;


@RestController
@RequestMapping("${api.base-path}" + Routes.Channel.basePath)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChannelResource {
    String basePath;
    ChannelService channelService;

    public ChannelResource(@Value("${api.base-path}") String basePath, ChannelService channelService) {
        this.basePath = basePath + Routes.Channel.basePath;
        this.channelService = channelService;
    }

    @GetMapping
    public ResponseEntity<Mono<BaseResponse<List<ChannelDTO>>>> getChannels() {
        return ResponseEntity.ok(channelService.getAllChannels());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mono<BaseResponse<ChannelDTO>>> getChannelByIdOrUsername(@PathVariable("id") String id) {
        return ResponseEntity.ok(channelService.getChannelByIdOrPathName(id));
    }

    @PostMapping
    public Mono<ResponseEntity<BaseResponse<ChannelDTO>>> createChannel(@RequestBody @Valid CreateChannelDTO dto) {
        return channelService.createChannelInfo(dto)
                .map((res) -> {
                    long id = res.getData().getId();

                    return ResponseEntity.created(URI.create(STR."\{basePath}/\{id}")).body(res);
                });
    }

    @PutMapping("/{id}")
    public ResponseEntity<Mono<BaseResponse<ChannelDTO>>> updateChannel(@PathVariable("id") String id, @RequestBody @Valid UpdateChannelDTO dto) {
        return ResponseEntity.ok(channelService.updateChannelInfo(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Mono<BaseResponse<Void>>> deleteChannel(@PathVariable("id") String id) {
        return ResponseEntity.ok(channelService.deleteChannel(id));
    }
}
