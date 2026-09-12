package no.xcello.matrikkel;

import jakarta.xml.ws.BindingProvider;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.MatrikkelContext;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.geometri.koder.KoordinatsystemKodeId;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.service.adresse.AdresseService;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.service.adresse.AdresseServiceWS;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.service.store.StoreService;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.service.store.StoreServiceWS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:matrikkel.properties", ignoreResourceNotFound = true)
public class MatrikkelConfig {

    @Bean
    AdresseService adresseService(
            @Value("${matrikkel.ws.username}") String username,
            @Value("${matrikkel.ws.password}") String password
    ) {
        final AdresseServiceWS ws = new AdresseServiceWS();
        final AdresseService out = ws.getAdresseServicePort();
        ((BindingProvider) out).getRequestContext().put(BindingProvider.USERNAME_PROPERTY, username);
        ((BindingProvider) out).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, password);
        return out;
    }

    @Bean
    StoreService storeService(
            @Value("${matrikkel.ws.username}") String username,
            @Value("${matrikkel.ws.password}") String password
    ) {
        final StoreServiceWS ws = new StoreServiceWS();
        final StoreService out = ws.getStoreServicePort();
        ((BindingProvider) out).getRequestContext().put(BindingProvider.USERNAME_PROPERTY, username);
        ((BindingProvider) out).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, password);
        return out;
    }

    @Bean
    MatrikkelContext matrikkelContext() {
        MatrikkelContext context = new MatrikkelContext();
        context.setLocale("no_NO_B");
        final KoordinatsystemKodeId k = new KoordinatsystemKodeId();
        k.setValue(10L);
        context.setKoordinatsystemKodeId(k);
        context.setKlientIdentifikasjon("xcello");
        context.setSystemVersion("4.18");
        return context;
    }
}
