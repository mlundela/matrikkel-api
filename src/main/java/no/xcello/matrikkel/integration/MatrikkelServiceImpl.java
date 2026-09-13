package no.xcello.matrikkel.integration;

import no.xcello.matrikkel.core.Section;
import no.xcello.matrikkel.core.MatrikkelService;

import java.util.List;

@org.springframework.stereotype.Service
public class MatrikkelServiceImpl implements MatrikkelService {

    private final MatrikkelClient webServiceClient;

    MatrikkelServiceImpl(MatrikkelClient webServiceClient) {
        this.webServiceClient = webServiceClient;
    }

    @Override
    public List<Section> listSections(String matrikkelId) {
        return webServiceClient.listSections(matrikkelId);
    }
}
