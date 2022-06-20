package se.terrassorkestern.notgen.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import se.terrassorkestern.notgen.model.Progress;

@Component
public class ProgressService {

    final SimpMessagingTemplate simpMessagingTemplate;

    public ProgressService(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public void updateProgress(Progress progress) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            String currentUserName = authentication.getName();
            simpMessagingTemplate.convertAndSendToUser(currentUserName, "/queue/progress", progress);
        }
    }

}
