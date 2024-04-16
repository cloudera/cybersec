package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.queryservice.common.exception.FailedAllClusterReponseException;
import com.cloudera.parserchains.queryservice.common.exception.FailedClusterReponseException;
import com.cloudera.service.common.response.ResponseBody;
import com.cloudera.service.common.response.ResponseType;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClusterServiceTest {
    @Mock
    private KafkaService kafkaService;

    @InjectMocks
    private ClusterService clusterService;


    @Test
    void testGetAllClusterInfoSuccess() throws Exception {
        Pair<ResponseType, ResponseBody> pair = Pair.of(ResponseType.GET_ALL_CLUSTERS_SERVICE_RESPONSE, new ResponseBody());
        when(kafkaService.sendWithReply(any(), any())).thenReturn(Collections.singletonList(pair));

        List<ResponseBody> result = clusterService.getAllClusterInfo();

        assertThat(result).hasSize(1);
    }

    @Test
    void testGetAllClusterInfoFailure() {
        Pair<ResponseType, ResponseBody> pair1 = Pair.of(ResponseType.GET_ALL_CLUSTERS_SERVICE_RESPONSE, new ResponseBody());
        Pair<ResponseType, ResponseBody> pair2 = Pair.of(ResponseType.ERROR_RESPONSE, new ResponseBody());
        when(kafkaService.sendWithReply(any(), any())).thenReturn(Arrays.asList(pair1, pair2));

        FailedAllClusterReponseException exception = assertThrows(FailedAllClusterReponseException.class, () -> {
            clusterService.getAllClusterInfo();
        });

        assertThat(exception).isNotNull();
    }

    @Test
    void testGetClusterInfoSuccess() throws Exception {
        Pair<ResponseType, ResponseBody> pair = Pair.of(ResponseType.GET_CLUSTER_SERVICE_RESPONSE, new ResponseBody());
        when(kafkaService.sendWithReply(any(), any(), any())).thenReturn(pair);

        ResponseBody result = clusterService.getClusterInfo("someClusterId");

        assertThat(result).isNotNull();
    }

    @Test
    void testGetClusterInfoFailure() {
        Pair<ResponseType, ResponseBody> pair = Pair.of(ResponseType.ERROR_RESPONSE, new ResponseBody());
        when(kafkaService.sendWithReply(any(), any(), any())).thenReturn(pair);

        FailedClusterReponseException exception = assertThrows(FailedClusterReponseException.class, () -> {
            clusterService.getClusterInfo("someClusterId");
        });

        assertThat(exception).isNotNull();
    }
}