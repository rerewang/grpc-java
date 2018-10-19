// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: grpc/gcp/handshaker.proto

package io.grpc.alts.internal;

public interface HandshakerResultOrBuilder extends
    // @@protoc_insertion_point(interface_extends:grpc.gcp.HandshakerResult)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * The application protocol negotiated for this connection.
   * </pre>
   *
   * <code>string application_protocol = 1;</code>
   */
  java.lang.String getApplicationProtocol();
  /**
   * <pre>
   * The application protocol negotiated for this connection.
   * </pre>
   *
   * <code>string application_protocol = 1;</code>
   */
  com.google.protobuf.ByteString
      getApplicationProtocolBytes();

  /**
   * <pre>
   * The record protocol negotiated for this connection.
   * </pre>
   *
   * <code>string record_protocol = 2;</code>
   */
  java.lang.String getRecordProtocol();
  /**
   * <pre>
   * The record protocol negotiated for this connection.
   * </pre>
   *
   * <code>string record_protocol = 2;</code>
   */
  com.google.protobuf.ByteString
      getRecordProtocolBytes();

  /**
   * <pre>
   * Cryptographic key data. The key data may be more than the key length
   * required for the record protocol, thus the client of the handshaker
   * service needs to truncate the key data into the right key length.
   * </pre>
   *
   * <code>bytes key_data = 3;</code>
   */
  com.google.protobuf.ByteString getKeyData();

  /**
   * <pre>
   * The authenticated identity of the peer.
   * </pre>
   *
   * <code>.grpc.gcp.Identity peer_identity = 4;</code>
   */
  boolean hasPeerIdentity();
  /**
   * <pre>
   * The authenticated identity of the peer.
   * </pre>
   *
   * <code>.grpc.gcp.Identity peer_identity = 4;</code>
   */
  io.grpc.alts.internal.Identity getPeerIdentity();
  /**
   * <pre>
   * The authenticated identity of the peer.
   * </pre>
   *
   * <code>.grpc.gcp.Identity peer_identity = 4;</code>
   */
  io.grpc.alts.internal.IdentityOrBuilder getPeerIdentityOrBuilder();

  /**
   * <pre>
   * The local identity used in the handshake.
   * </pre>
   *
   * <code>.grpc.gcp.Identity local_identity = 5;</code>
   */
  boolean hasLocalIdentity();
  /**
   * <pre>
   * The local identity used in the handshake.
   * </pre>
   *
   * <code>.grpc.gcp.Identity local_identity = 5;</code>
   */
  io.grpc.alts.internal.Identity getLocalIdentity();
  /**
   * <pre>
   * The local identity used in the handshake.
   * </pre>
   *
   * <code>.grpc.gcp.Identity local_identity = 5;</code>
   */
  io.grpc.alts.internal.IdentityOrBuilder getLocalIdentityOrBuilder();

  /**
   * <pre>
   * Indicate whether the handshaker service client should keep the channel
   * between the handshaker service open, e.g., in order to handle
   * post-handshake messages in the future.
   * </pre>
   *
   * <code>bool keep_channel_open = 6;</code>
   */
  boolean getKeepChannelOpen();

  /**
   * <pre>
   * The RPC protocol versions supported by the peer.
   * </pre>
   *
   * <code>.grpc.gcp.RpcProtocolVersions peer_rpc_versions = 7;</code>
   */
  boolean hasPeerRpcVersions();
  /**
   * <pre>
   * The RPC protocol versions supported by the peer.
   * </pre>
   *
   * <code>.grpc.gcp.RpcProtocolVersions peer_rpc_versions = 7;</code>
   */
  io.grpc.alts.internal.RpcProtocolVersions getPeerRpcVersions();
  /**
   * <pre>
   * The RPC protocol versions supported by the peer.
   * </pre>
   *
   * <code>.grpc.gcp.RpcProtocolVersions peer_rpc_versions = 7;</code>
   */
  io.grpc.alts.internal.RpcProtocolVersionsOrBuilder getPeerRpcVersionsOrBuilder();
}
