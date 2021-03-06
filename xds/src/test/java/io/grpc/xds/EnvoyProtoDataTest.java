/*
 * Copyright 2019 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.xds;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.testing.EqualsTester;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Struct;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.Value;
import com.google.protobuf.util.Durations;
import com.google.re2j.Pattern;
import io.envoyproxy.envoy.config.core.v3.RuntimeFractionalPercent;
import io.envoyproxy.envoy.config.route.v3.QueryParameterMatcher;
import io.envoyproxy.envoy.config.route.v3.RedirectAction;
import io.envoyproxy.envoy.config.route.v3.WeightedCluster;
import io.envoyproxy.envoy.type.matcher.v3.RegexMatcher;
import io.envoyproxy.envoy.type.v3.FractionalPercent;
import io.envoyproxy.envoy.type.v3.Int64Range;
import io.grpc.xds.EnvoyProtoData.Address;
import io.grpc.xds.EnvoyProtoData.ClusterWeight;
import io.grpc.xds.EnvoyProtoData.Locality;
import io.grpc.xds.EnvoyProtoData.Node;
import io.grpc.xds.EnvoyProtoData.Route;
import io.grpc.xds.EnvoyProtoData.RouteAction;
import io.grpc.xds.EnvoyProtoData.StructOrError;
import io.grpc.xds.RouteMatch.FractionMatcher;
import io.grpc.xds.RouteMatch.HeaderMatcher;
import io.grpc.xds.RouteMatch.PathMatcher;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link EnvoyProtoData}.
 */
@RunWith(JUnit4.class)
public class EnvoyProtoDataTest {

  @Test
  public void locality_convertToAndFromLocalityProto() {
    io.envoyproxy.envoy.config.core.v3.Locality locality =
        io.envoyproxy.envoy.config.core.v3.Locality.newBuilder()
            .setRegion("test_region")
            .setZone("test_zone")
            .setSubZone("test_subzone")
            .build();
    Locality xdsLocality = Locality.fromEnvoyProtoLocality(locality);
    assertThat(xdsLocality.getRegion()).isEqualTo("test_region");
    assertThat(xdsLocality.getZone()).isEqualTo("test_zone");
    assertThat(xdsLocality.getSubZone()).isEqualTo("test_subzone");

    io.envoyproxy.envoy.api.v2.core.Locality convertedLocality =
        xdsLocality.toEnvoyProtoLocalityV2();
    assertThat(convertedLocality.getRegion()).isEqualTo("test_region");
    assertThat(convertedLocality.getZone()).isEqualTo("test_zone");
    assertThat(convertedLocality.getSubZone()).isEqualTo("test_subzone");
  }

  @Test
  public void locality_equal() {
    new EqualsTester()
        .addEqualityGroup(
            new Locality("region-a", "zone-a", "subzone-a"),
            new Locality("region-a", "zone-a", "subzone-a"))
        .addEqualityGroup(
            new Locality("region", "zone", "subzone")
        )
        .addEqualityGroup(
            new Locality("", "", ""),
            new Locality("", "", ""))
        .testEquals();
  }

  @Test
  public void convertNode() {
    Node node = Node.newBuilder()
        .setId("node-id")
        .setCluster("cluster")
        .setMetadata(
            ImmutableMap.of(
                "TRAFFICDIRECTOR_INTERCEPTION_PORT",
                "ENVOY_PORT",
                "TRAFFICDIRECTOR_NETWORK_NAME",
                "VPC_NETWORK_NAME"))
        .setLocality(new Locality("region", "zone", "subzone"))
        .addListeningAddresses(new Address("www.foo.com", 8080))
        .addListeningAddresses(new Address("www.bar.com", 8088))
        .setBuildVersion("v1")
        .setUserAgentName("agent")
        .setUserAgentVersion("1.1")
        .addClientFeatures("feature-1")
        .addClientFeatures("feature-2")
        .build();
    io.envoyproxy.envoy.config.core.v3.Node nodeProto =
        io.envoyproxy.envoy.config.core.v3.Node.newBuilder()
            .setId("node-id")
            .setCluster("cluster")
            .setMetadata(Struct.newBuilder()
                .putFields("TRAFFICDIRECTOR_INTERCEPTION_PORT",
                    Value.newBuilder().setStringValue("ENVOY_PORT").build())
                .putFields("TRAFFICDIRECTOR_NETWORK_NAME",
                    Value.newBuilder().setStringValue("VPC_NETWORK_NAME").build()))
            .setLocality(
                io.envoyproxy.envoy.config.core.v3.Locality.newBuilder()
                    .setRegion("region")
                    .setZone("zone")
                    .setSubZone("subzone"))
            .addListeningAddresses(
                io.envoyproxy.envoy.config.core.v3.Address.newBuilder()
                    .setSocketAddress(
                        io.envoyproxy.envoy.config.core.v3.SocketAddress.newBuilder()
                            .setAddress("www.foo.com")
                            .setPortValue(8080)))
            .addListeningAddresses(
                io.envoyproxy.envoy.config.core.v3.Address.newBuilder()
                    .setSocketAddress(
                        io.envoyproxy.envoy.config.core.v3.SocketAddress.newBuilder()
                            .setAddress("www.bar.com")
                            .setPortValue(8088)))
            .setUserAgentName("agent")
            .setUserAgentVersion("1.1")
            .addClientFeatures("feature-1")
            .addClientFeatures("feature-2")
            .build();
    assertThat(node.toEnvoyProtoNode()).isEqualTo(nodeProto);

    @SuppressWarnings("deprecation") // Deprecated v2 API setBuildVersion().
    io.envoyproxy.envoy.api.v2.core.Node nodeProtoV2 =
        io.envoyproxy.envoy.api.v2.core.Node.newBuilder()
            .setId("node-id")
            .setCluster("cluster")
            .setMetadata(Struct.newBuilder()
                .putFields("TRAFFICDIRECTOR_INTERCEPTION_PORT",
                    Value.newBuilder().setStringValue("ENVOY_PORT").build())
                .putFields("TRAFFICDIRECTOR_NETWORK_NAME",
                    Value.newBuilder().setStringValue("VPC_NETWORK_NAME").build()))
            .setLocality(
                io.envoyproxy.envoy.api.v2.core.Locality.newBuilder()
                    .setRegion("region")
                    .setZone("zone")
                    .setSubZone("subzone"))
            .addListeningAddresses(
                io.envoyproxy.envoy.api.v2.core.Address.newBuilder()
                    .setSocketAddress(
                        io.envoyproxy.envoy.api.v2.core.SocketAddress.newBuilder()
                            .setAddress("www.foo.com")
                            .setPortValue(8080)))
            .addListeningAddresses(
                io.envoyproxy.envoy.api.v2.core.Address.newBuilder()
                    .setSocketAddress(
                        io.envoyproxy.envoy.api.v2.core.SocketAddress.newBuilder()
                            .setAddress("www.bar.com")
                            .setPortValue(8088)))
            .setBuildVersion("v1")
            .setUserAgentName("agent")
            .setUserAgentVersion("1.1")
            .addClientFeatures("feature-1")
            .addClientFeatures("feature-2")
            .build();
    assertThat(node.toEnvoyProtoNodeV2()).isEqualTo(nodeProtoV2);
  }

  @Test
  public void locality_hash() {
    assertThat(new Locality("region", "zone", "subzone").hashCode())
        .isEqualTo(new Locality("region", "zone","subzone").hashCode());
  }

  // TODO(chengyuanzhang): add test for other data types.

  @Test
  public void convertRoute() {
    io.envoyproxy.envoy.config.route.v3.Route proto1 =
        io.envoyproxy.envoy.config.route.v3.Route.newBuilder()
            .setName("route-blade")
            .setMatch(
                io.envoyproxy.envoy.config.route.v3.RouteMatch.newBuilder()
                    .setPath("/service/method"))
            .setRoute(
                io.envoyproxy.envoy.config.route.v3.RouteAction.newBuilder()
                    .setCluster("cluster-foo"))
            .build();
    StructOrError<Route> struct1 = Route.fromEnvoyProtoRoute(proto1);
    assertThat(struct1.getErrorDetail()).isNull();
    assertThat(struct1.getStruct())
        .isEqualTo(
            new Route(
                new RouteMatch(new PathMatcher("/service/method", null, null),
                    Collections.<HeaderMatcher>emptyList(), null),
                new RouteAction(TimeUnit.SECONDS.toNanos(15L), "cluster-foo", null)));

    io.envoyproxy.envoy.config.route.v3.Route unsupportedProto =
        io.envoyproxy.envoy.config.route.v3.Route.newBuilder()
            .setName("route-blade")
            .setMatch(io.envoyproxy.envoy.config.route.v3.RouteMatch.newBuilder().setPath(""))
            .setRedirect(RedirectAction.getDefaultInstance())
            .build();
    StructOrError<Route> unsupportedStruct = Route.fromEnvoyProtoRoute(unsupportedProto);
    assertThat(unsupportedStruct.getErrorDetail()).isNotNull();
    assertThat(unsupportedStruct.getStruct()).isNull();
  }

  @Test
  public void convertRoute_skipWithUnsupportedMatcher() {
    io.envoyproxy.envoy.config.route.v3.Route proto =
        io.envoyproxy.envoy.config.route.v3.Route.newBuilder()
            .setName("ignore me")
            .setMatch(
                io.envoyproxy.envoy.config.route.v3.RouteMatch.newBuilder()
                    .setPath("/service/method")
                    .addQueryParameters(
                        io.envoyproxy.envoy.config.route.v3.QueryParameterMatcher
                            .getDefaultInstance()))
            .setRoute(
                io.envoyproxy.envoy.config.route.v3.RouteAction.newBuilder()
                    .setCluster("cluster-foo"))
            .build();
    assertThat(Route.fromEnvoyProtoRoute(proto)).isNull();
  }

  @Test
  public void convertRoute_skipWithUnsupportedAction() {
    io.envoyproxy.envoy.config.route.v3.Route proto =
        io.envoyproxy.envoy.config.route.v3.Route.newBuilder()
            .setName("ignore me")
            .setMatch(
                io.envoyproxy.envoy.config.route.v3.RouteMatch.newBuilder()
                    .setPath("/service/method"))
            .setRoute(
                io.envoyproxy.envoy.config.route.v3.RouteAction.newBuilder()
                    .setClusterHeader("some cluster header"))
            .build();
    assertThat(Route.fromEnvoyProtoRoute(proto)).isNull();
  }

  @Test
  public void isDefaultRoute() {
    StructOrError<Route> struct1 = Route.fromEnvoyProtoRoute(buildSimpleRouteProto("", null));
    StructOrError<Route> struct2 = Route.fromEnvoyProtoRoute(buildSimpleRouteProto("/", null));
    StructOrError<Route> struct3 =
        Route.fromEnvoyProtoRoute(buildSimpleRouteProto("/service/", null));
    StructOrError<Route> struct4 =
        Route.fromEnvoyProtoRoute(buildSimpleRouteProto(null, "/service/method"));

    assertThat(struct1.getStruct().isDefaultRoute()).isTrue();
    assertThat(struct2.getStruct().isDefaultRoute()).isTrue();
    assertThat(struct3.getStruct().isDefaultRoute()).isFalse();
    assertThat(struct4.getStruct().isDefaultRoute()).isFalse();
  }

  private static io.envoyproxy.envoy.config.route.v3.Route buildSimpleRouteProto(
      @Nullable String pathPrefix, @Nullable String path) {
    io.envoyproxy.envoy.config.route.v3.Route.Builder routeBuilder =
        io.envoyproxy.envoy.config.route.v3.Route.newBuilder()
            .setName("simple-route")
            .setRoute(io.envoyproxy.envoy.config.route.v3.RouteAction.newBuilder()
            .setCluster("simple-cluster"));
    if (pathPrefix != null) {
      routeBuilder.setMatch(io.envoyproxy.envoy.config.route.v3.RouteMatch.newBuilder()
          .setPrefix(pathPrefix));
    } else if (path != null) {
      routeBuilder.setMatch(io.envoyproxy.envoy.config.route.v3.RouteMatch.newBuilder()
          .setPath(path));
    }
    return routeBuilder.build();
  }

  @Test
  public void convertRouteMatch_pathMatching() {
    // path_specifier = prefix
    io.envoyproxy.envoy.config.route.v3.RouteMatch proto1 =
        io.envoyproxy.envoy.config.route.v3.RouteMatch.newBuilder().setPrefix("/").build();
    StructOrError<RouteMatch> struct1 = Route.convertEnvoyProtoRouteMatch(proto1);
    assertThat(struct1.getErrorDetail()).isNull();
    assertThat(struct1.getStruct()).isEqualTo(
        new RouteMatch(
            new PathMatcher(null, "/", null), Collections.<HeaderMatcher>emptyList(), null));

    // path_specifier = path
    io.envoyproxy.envoy.config.route.v3.RouteMatch proto2 =
        io.envoyproxy.envoy.config.route.v3.RouteMatch.newBuilder()
            .setPath("/service/method")
            .build();
    StructOrError<RouteMatch> struct2 = Route.convertEnvoyProtoRouteMatch(proto2);
    assertThat(struct2.getErrorDetail()).isNull();
    assertThat(struct2.getStruct()).isEqualTo(
        new RouteMatch(
            new PathMatcher("/service/method", null, null),
            Collections.<HeaderMatcher>emptyList(), null));

    // path_specifier = safe_regex
    io.envoyproxy.envoy.config.route.v3.RouteMatch proto4 =
        io.envoyproxy.envoy.config.route.v3.RouteMatch.newBuilder()
            .setSafeRegex(RegexMatcher.newBuilder().setRegex("."))
            .build();
    StructOrError<RouteMatch> struct4 = Route.convertEnvoyProtoRouteMatch(proto4);
    assertThat(struct4.getErrorDetail()).isNull();
    assertThat(struct4.getStruct()).isEqualTo(
        new RouteMatch(
            new PathMatcher(null, null, Pattern.compile(".")),
            Collections.<HeaderMatcher>emptyList(), null));

    // case_sensitive = false
    io.envoyproxy.envoy.config.route.v3.RouteMatch proto5 =
        io.envoyproxy.envoy.config.route.v3.RouteMatch.newBuilder()
            .setCaseSensitive(BoolValue.newBuilder().setValue(false))
            .build();
    StructOrError<RouteMatch> struct5 = Route.convertEnvoyProtoRouteMatch(proto5);
    assertThat(struct5.getErrorDetail()).isNotNull();
    assertThat(struct5.getStruct()).isNull();

    // query_parameters is set
    io.envoyproxy.envoy.config.route.v3.RouteMatch proto6 =
        io.envoyproxy.envoy.config.route.v3.RouteMatch.newBuilder()
            .addQueryParameters(QueryParameterMatcher.getDefaultInstance())
            .build();
    StructOrError<RouteMatch> struct6 = Route.convertEnvoyProtoRouteMatch(proto6);
    assertThat(struct6).isNull();

    // path_specifier unset
    io.envoyproxy.envoy.config.route.v3.RouteMatch unsetProto =
        io.envoyproxy.envoy.config.route.v3.RouteMatch.getDefaultInstance();
    StructOrError<RouteMatch> unsetStruct = Route.convertEnvoyProtoRouteMatch(unsetProto);
    assertThat(unsetStruct.getErrorDetail()).isNotNull();
    assertThat(unsetStruct.getStruct()).isNull();
  }

  @Test
  public void convertRouteMatch_withHeaderMatching() {
    io.envoyproxy.envoy.config.route.v3.RouteMatch proto =
        io.envoyproxy.envoy.config.route.v3.RouteMatch.newBuilder()
            .setPrefix("")
            .addHeaders(
                io.envoyproxy.envoy.config.route.v3.HeaderMatcher.newBuilder()
                    .setName(":scheme")
                    .setPrefixMatch("http"))
            .addHeaders(
                io.envoyproxy.envoy.config.route.v3.HeaderMatcher.newBuilder()
                    .setName(":method")
                    .setExactMatch("PUT"))
            .build();
    StructOrError<RouteMatch> struct = Route.convertEnvoyProtoRouteMatch(proto);
    assertThat(struct.getErrorDetail()).isNull();
    assertThat(struct.getStruct())
        .isEqualTo(
            new RouteMatch(
                new PathMatcher(null, "", null),
                Arrays.asList(
                    new HeaderMatcher(":scheme", null, null, null, null, "http", null, false),
                    new HeaderMatcher(":method", "PUT", null, null, null, null, null, false)),
                null));
  }

  @Test
  public void convertRouteMatch_withRuntimeFraction() {
    io.envoyproxy.envoy.config.route.v3.RouteMatch proto =
        io.envoyproxy.envoy.config.route.v3.RouteMatch.newBuilder()
            .setPrefix("")
            .setRuntimeFraction(
                RuntimeFractionalPercent.newBuilder()
                    .setDefaultValue(
                        FractionalPercent.newBuilder()
                            .setNumerator(30)
                            .setDenominator(FractionalPercent.DenominatorType.HUNDRED)))
            .build();
    StructOrError<RouteMatch> struct = Route.convertEnvoyProtoRouteMatch(proto);
    assertThat(struct.getErrorDetail()).isNull();
    assertThat(struct.getStruct())
        .isEqualTo(
            new RouteMatch(
                new PathMatcher(null, "", null), Collections.<HeaderMatcher>emptyList(),
                new FractionMatcher(30, 100)));
  }

  @Test
  public void convertRouteAction() {
    // cluster_specifier = cluster, default timeout
    io.envoyproxy.envoy.config.route.v3.RouteAction proto1 =
        io.envoyproxy.envoy.config.route.v3.RouteAction.newBuilder()
            .setCluster("cluster-foo")
            .build();
    StructOrError<RouteAction> struct1 = RouteAction.fromEnvoyProtoRouteAction(proto1);
    assertThat(struct1.getErrorDetail()).isNull();
    assertThat(struct1.getStruct().getTimeoutNano())
        .isEqualTo(TimeUnit.SECONDS.toNanos(15L)); // default value
    assertThat(struct1.getStruct().getCluster()).isEqualTo("cluster-foo");
    assertThat(struct1.getStruct().getWeightedCluster()).isNull();

    // cluster_specifier = cluster, infinity timeout
    io.envoyproxy.envoy.config.route.v3.RouteAction proto2 =
        io.envoyproxy.envoy.config.route.v3.RouteAction.newBuilder()
            .setMaxGrpcTimeout(Durations.fromNanos(0))
            .setTimeout(Durations.fromMicros(20L))
            .setCluster("cluster-foo")
            .build();
    StructOrError<RouteAction> struct2 = RouteAction.fromEnvoyProtoRouteAction(proto2);
    assertThat(struct2.getStruct().getTimeoutNano())
        .isEqualTo(Long.MAX_VALUE); // infinite

    // cluster_specifier = cluster, infinity timeout
    io.envoyproxy.envoy.config.route.v3.RouteAction proto3 =
        io.envoyproxy.envoy.config.route.v3.RouteAction.newBuilder()
            .setTimeout(Durations.fromNanos(0))
            .setCluster("cluster-foo")
            .build();
    StructOrError<RouteAction> struct3 = RouteAction.fromEnvoyProtoRouteAction(proto3);
    assertThat(struct3.getStruct().getTimeoutNano()).isEqualTo(Long.MAX_VALUE); // infinite

    // cluster_specifier = cluster_header
    io.envoyproxy.envoy.config.route.v3.RouteAction proto4 =
        io.envoyproxy.envoy.config.route.v3.RouteAction.newBuilder()
            .setClusterHeader("cluster-bar")
            .build();
    StructOrError<RouteAction> struct4 = RouteAction.fromEnvoyProtoRouteAction(proto4);
    assertThat(struct4).isNull();

    // cluster_specifier = weighted_cluster
    io.envoyproxy.envoy.config.route.v3.RouteAction proto5 =
        io.envoyproxy.envoy.config.route.v3.RouteAction.newBuilder()
            .setMaxGrpcTimeout(Durations.fromSeconds(6L))
            .setTimeout(Durations.fromMicros(20L))
            .setWeightedClusters(
                WeightedCluster.newBuilder()
                    .addClusters(
                        WeightedCluster.ClusterWeight
                            .newBuilder()
                            .setName("cluster-baz")
                            .setWeight(UInt32Value.newBuilder().setValue(100))))
            .build();
    StructOrError<RouteAction> struct5 = RouteAction.fromEnvoyProtoRouteAction(proto5);
    assertThat(struct5.getErrorDetail()).isNull();
    assertThat(struct5.getStruct().getTimeoutNano())
        .isEqualTo(TimeUnit.SECONDS.toNanos(6L));
    assertThat(struct5.getStruct().getCluster()).isNull();
    assertThat(struct5.getStruct().getWeightedCluster())
        .containsExactly(new ClusterWeight("cluster-baz", 100));

    // cluster_specifier unset
    io.envoyproxy.envoy.config.route.v3.RouteAction unsetProto =
        io.envoyproxy.envoy.config.route.v3.RouteAction.getDefaultInstance();
    StructOrError<RouteAction> unsetStruct = RouteAction.fromEnvoyProtoRouteAction(unsetProto);
    assertThat(unsetStruct.getErrorDetail()).isNotNull();
    assertThat(unsetStruct.getStruct()).isNull();
  }

  @Test
  public void convertHeaderMatcher() {
    // header_match_specifier = exact_match
    io.envoyproxy.envoy.config.route.v3.HeaderMatcher proto1 =
        io.envoyproxy.envoy.config.route.v3.HeaderMatcher.newBuilder()
            .setName(":method")
            .setExactMatch("PUT")
            .build();
    StructOrError<HeaderMatcher> struct1 = Route.convertEnvoyProtoHeaderMatcher(proto1);
    assertThat(struct1.getErrorDetail()).isNull();
    assertThat(struct1.getStruct()).isEqualTo(
        new HeaderMatcher(":method", "PUT", null, null, null, null, null, false));

    // header_match_specifier = safe_regex_match
    io.envoyproxy.envoy.config.route.v3.HeaderMatcher proto3 =
        io.envoyproxy.envoy.config.route.v3.HeaderMatcher.newBuilder()
            .setName(":method")
            .setSafeRegexMatch(RegexMatcher.newBuilder().setRegex("P*"))
            .build();
    StructOrError<HeaderMatcher> struct3 = Route.convertEnvoyProtoHeaderMatcher(proto3);
    assertThat(struct3.getErrorDetail()).isNull();
    assertThat(struct3.getStruct()).isEqualTo(
        new HeaderMatcher(":method", null, Pattern.compile("P*"), null, null, null, null, false));

    // header_match_specifier = range_match
    io.envoyproxy.envoy.config.route.v3.HeaderMatcher proto4 =
        io.envoyproxy.envoy.config.route.v3.HeaderMatcher.newBuilder()
            .setName("timeout")
            .setRangeMatch(Int64Range.newBuilder().setStart(10L).setEnd(20L))
            .build();
    StructOrError<HeaderMatcher> struct4 = Route.convertEnvoyProtoHeaderMatcher(proto4);
    assertThat(struct4.getErrorDetail()).isNull();
    assertThat(struct4.getStruct()).isEqualTo(
        new HeaderMatcher(
            "timeout", null, null, new HeaderMatcher.Range(10L, 20L), null, null, null, false));

    // header_match_specifier = present_match
    io.envoyproxy.envoy.config.route.v3.HeaderMatcher proto5 =
        io.envoyproxy.envoy.config.route.v3.HeaderMatcher.newBuilder()
            .setName("user-agent")
            .setPresentMatch(true)
            .build();
    StructOrError<HeaderMatcher> struct5 = Route.convertEnvoyProtoHeaderMatcher(proto5);
    assertThat(struct5.getErrorDetail()).isNull();
    assertThat(struct5.getStruct()).isEqualTo(
        new HeaderMatcher("user-agent", null, null, null, true, null, null, false));

    // header_match_specifier = prefix_match
    io.envoyproxy.envoy.config.route.v3.HeaderMatcher proto6 =
        io.envoyproxy.envoy.config.route.v3.HeaderMatcher.newBuilder()
            .setName("authority")
            .setPrefixMatch("service-foo")
            .build();
    StructOrError<HeaderMatcher> struct6 = Route.convertEnvoyProtoHeaderMatcher(proto6);
    assertThat(struct6.getErrorDetail()).isNull();
    assertThat(struct6.getStruct()).isEqualTo(
        new HeaderMatcher("authority", null, null, null, null, "service-foo", null, false));

    // header_match_specifier = suffix_match
    io.envoyproxy.envoy.config.route.v3.HeaderMatcher proto7 =
        io.envoyproxy.envoy.config.route.v3.HeaderMatcher.newBuilder()
            .setName("authority")
            .setSuffixMatch("googleapis.com")
            .build();
    StructOrError<HeaderMatcher> struct7 = Route.convertEnvoyProtoHeaderMatcher(proto7);
    assertThat(struct7.getErrorDetail()).isNull();
    assertThat(struct7.getStruct()).isEqualTo(
        new HeaderMatcher(
            "authority", null, null, null, null, null, "googleapis.com", false));

    // header_match_specifier unset
    io.envoyproxy.envoy.config.route.v3.HeaderMatcher unsetProto =
        io.envoyproxy.envoy.config.route.v3.HeaderMatcher.getDefaultInstance();
    StructOrError<HeaderMatcher> unsetStruct = Route.convertEnvoyProtoHeaderMatcher(unsetProto);
    assertThat(unsetStruct.getErrorDetail()).isNotNull();
    assertThat(unsetStruct.getStruct()).isNull();
  }

  @Test
  public void convertHeaderMatcher_malformedRegExPattern() {
    io.envoyproxy.envoy.config.route.v3.HeaderMatcher proto =
        io.envoyproxy.envoy.config.route.v3.HeaderMatcher.newBuilder()
            .setName(":method")
            .setSafeRegexMatch(RegexMatcher.newBuilder().setRegex("["))
            .build();
    StructOrError<HeaderMatcher> struct = Route.convertEnvoyProtoHeaderMatcher(proto);
    assertThat(struct.getErrorDetail()).isNotNull();
    assertThat(struct.getStruct()).isNull();
  }

  @Test
  public void convertClusterWeight() {
    io.envoyproxy.envoy.config.route.v3.WeightedCluster.ClusterWeight proto =
        io.envoyproxy.envoy.config.route.v3.WeightedCluster.ClusterWeight.newBuilder()
            .setName("cluster-foo")
            .setWeight(UInt32Value.newBuilder().setValue(30)).build();
    ClusterWeight struct = ClusterWeight.fromEnvoyProtoClusterWeight(proto);
    assertThat(struct.getName()).isEqualTo("cluster-foo");
    assertThat(struct.getWeight()).isEqualTo(30);
  }
}
