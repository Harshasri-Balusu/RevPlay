package com.revplay.musicplatform.ai.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.ai.service.GrokService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AIServiceImplTest {

    private static final String SUPPORT_QUERY = "How can I contact support email?";
    private static final String NORMAL_QUERY = "  what premium benefits do i get?  ";

    @Mock
    private GrokService grokService;

    private AIServiceImpl aiService;

    @BeforeEach
    void setUp() {
        aiService = new AIServiceImpl(grokService);
    }

    @Test
    @DisplayName("support query returns support message without calling Grok")
    void supportQueryBypassesGrok() {
        String response = aiService.getAIResponse(SUPPORT_QUERY);

        assertThat(response).isEqualTo("Please contact RevPlay support at revplay.support@gmail.com");
        verifyNoInteractions(grokService);
    }

    @Test
    @DisplayName("normal query builds prompt and cleans leaked assistant template text")
    void normalQueryBuildsPromptAndCleansResponse() {
        when(grokService.generateResponse(anyString()))
                .thenReturn("<|assistant|>\nAssistant: Premium removes ads. You can download songs offline");

        String response = aiService.getAIResponse(NORMAL_QUERY);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(grokService).generateResponse(promptCaptor.capture());

        assertThat(promptCaptor.getValue())
                .contains("<|system|>")
                .contains("<|user|>\nwhat premium benefits do i get?\n</s>")
                .contains("<|assistant|>");
        assertThat(response).isEqualTo("Premium removes ads.");
    }

    @Test
    @DisplayName("blank Grok response falls back to default message")
    void blankGrokResponseFallsBack() {
        when(grokService.generateResponse(anyString())).thenReturn("   ");

        String response = aiService.getAIResponse("Tell me about playlists");

        assertThat(response)
                .isEqualTo("I'm not sure about that yet. Please explore RevPlay features or contact support.");
    }
}
