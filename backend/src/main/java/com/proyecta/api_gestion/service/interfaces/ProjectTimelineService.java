package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.timeline.TimelineResponseDTO;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

public interface ProjectTimelineService {
    TimelineResponseDTO getProjectTimeline(String projectId);
    void uploadScheduleFile(String projectId, MultipartFile file);
    Resource downloadScheduleFile(String projectId);
}
