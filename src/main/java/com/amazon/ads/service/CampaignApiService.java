package com.amazon.ads.service;

import com.amazon.ads.adsApi.ApiClient;
import com.amazon.ads.adsApi.client.CampaignsApi;
import com.amazon.ads.adsApi.model.AdProduct;
import com.amazon.ads.adsApi.model.CampaignAdProductFilter;
import com.amazon.ads.adsApi.model.CampaignSuccessResponse;
import com.amazon.ads.adsApi.model.QueryCampaignRequest;
import com.amazon.ads.mcp.McpApplication;
import com.amazon.ads.util.ApiClientInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Service
public class CampaignApiService {

    private final ApiClientInitializer apiClientInitializer;
    private static final Logger log = LoggerFactory.getLogger(McpApplication.class);
    private final ApiClient apiClient;

    @Autowired
    public CampaignApiService(ApiClientInitializer apiClientInitializer) {
        // Configure API client
        log.debug("Initializing CampaignApiService");
        this.apiClientInitializer = apiClientInitializer;


        this.apiClient = new ApiClient();
        apiClient.setBasePath("https://advertising-api.amazon.com");
//        apiClient.setAccessToken(apiClientInitializer.getAccessToken());
        // Set the Amazon-Advertising-API-ClientId header

//        apiClient.addDefaultHeader("Amazon-Advertising-API-Scope", apiClientInitializer.getProfileId());
        // Set the Amazon-Advertising-API-Scope header (profile ID)

        // If your client library uses a specific authentication class for OAuth2, use that
//         OAuth2 oauth = (OAuth) apiClient.getAuthentication("bearerAuth"); // Replace "bearerAuth" with the correct security scheme name from your OAS
//         oauth.setAccessToken(accessToken);


        apiClient.addDefaultHeader("Authorization", apiClientInitializer.getAccessToken());
        apiClient.addDefaultHeader("Amazon-Advertising-API-ClientId", apiClientInitializer.getClientId());
//                getRefreshedToken(authMap.get(REFRESH_TOKEN_HEADER_NAME), authMap.get(CLIENT_ID_HEADER_NAME), authMap.get(CLIENT_SECRET_HEADER_NAME)));
//        campaignsApi.getApiClient().addDefaultHeader("Amazon-Advertising-API-ClientId", authMap.get(CLIENT_ID_HEADER_NAME));
        log.info("Querying CampaignApiService");
//        queryCampaigns(apiClientInitializer.ge);
        log.info("Initializing complete for CampaignApiService");
    }

    @GetMapping("/queryCampaigns")
    @Tool(description = "Get campaign data")
    public CampaignSuccessResponse queryCampaigns(String advertiserID) {
        try {
            log.info("Calling Query Campaigns {}", advertiserID);
            //TODO -Fix this
            CampaignsApi campaignsApi = new CampaignsApi(apiClient);
            CampaignSuccessResponse response = campaignsApi.queryCampaign(apiClientInitializer.getClientId(),
                    buildQueryRequest(), "", apiClientInitializer.getProfileId());

            log.info("API Client called successfully" + response);
            return response ;
        } catch (Exception e) {
            log.error("Failed to query campaign API", e);
            throw new RuntimeException("Failed to retried response");
        }
    }

    private QueryCampaignRequest buildQueryRequest() {
        QueryCampaignRequest queryCampaignRequest = new QueryCampaignRequest();
        CampaignAdProductFilter adProductFilter = new CampaignAdProductFilter();
        adProductFilter.setInclude(List.of(AdProduct.valueOf("SPONSORED_PRODUCTS")));
        queryCampaignRequest.setAdProductFilter(adProductFilter);

        queryCampaignRequest.setMaxResults(5000);
        queryCampaignRequest.setAdProductFilter(adProductFilter);
        return queryCampaignRequest;
    }

    public static void main(String[] args) {

    }
}
