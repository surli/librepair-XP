// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: google/protobuf/descriptor.proto at 202:1
package com.google.protobuf;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import com.squareup.wire.WireField;
import com.squareup.wire.internal.Internal;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.List;
import okio.ByteString;

/**
 * Describes an enum type.
 */
public final class EnumDescriptorProto extends Message<EnumDescriptorProto, EnumDescriptorProto.Builder> {
  public static final ProtoAdapter<EnumDescriptorProto> ADAPTER = new ProtoAdapter_EnumDescriptorProto();

  private static final long serialVersionUID = 0L;

  public static final String DEFAULT_NAME = "";

  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String name;

  @WireField(
      tag = 2,
      adapter = "com.google.protobuf.EnumValueDescriptorProto#ADAPTER",
      label = WireField.Label.REPEATED
  )
  public final List<EnumValueDescriptorProto> value;

  @WireField(
      tag = 3,
      adapter = "com.google.protobuf.EnumOptions#ADAPTER"
  )
  public final EnumOptions options;

  public EnumDescriptorProto(String name, List<EnumValueDescriptorProto> value, EnumOptions options) {
    this(name, value, options, ByteString.EMPTY);
  }

  public EnumDescriptorProto(String name, List<EnumValueDescriptorProto> value, EnumOptions options, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.name = name;
    this.value = Internal.immutableCopyOf("value", value);
    this.options = options;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.name = name;
    builder.value = Internal.copyOf("value", value);
    builder.options = options;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof EnumDescriptorProto)) return false;
    EnumDescriptorProto o = (EnumDescriptorProto) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(name, o.name)
        && value.equals(o.value)
        && Internal.equals(options, o.options);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (name != null ? name.hashCode() : 0);
      result = result * 37 + value.hashCode();
      result = result * 37 + (options != null ? options.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (name != null) builder.append(", name=").append(name);
    if (!value.isEmpty()) builder.append(", value=").append(value);
    if (options != null) builder.append(", options=").append(options);
    return builder.replace(0, 2, "EnumDescriptorProto{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<EnumDescriptorProto, Builder> {
    public String name;

    public List<EnumValueDescriptorProto> value;

    public EnumOptions options;

    public Builder() {
      value = Internal.newMutableList();
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder value(List<EnumValueDescriptorProto> value) {
      Internal.checkElementsNotNull(value);
      this.value = value;
      return this;
    }

    public Builder options(EnumOptions options) {
      this.options = options;
      return this;
    }

    @Override
    public EnumDescriptorProto build() {
      return new EnumDescriptorProto(name, value, options, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_EnumDescriptorProto extends ProtoAdapter<EnumDescriptorProto> {
    public ProtoAdapter_EnumDescriptorProto() {
      super(FieldEncoding.LENGTH_DELIMITED, EnumDescriptorProto.class);
    }

    @Override
    public int encodedSize(EnumDescriptorProto value) {
      return ProtoAdapter.STRING.encodedSizeWithTag(1, value.name)
          + EnumValueDescriptorProto.ADAPTER.asRepeated().encodedSizeWithTag(2, value.value)
          + EnumOptions.ADAPTER.encodedSizeWithTag(3, value.options)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, EnumDescriptorProto value) throws IOException {
      ProtoAdapter.STRING.encodeWithTag(writer, 1, value.name);
      EnumValueDescriptorProto.ADAPTER.asRepeated().encodeWithTag(writer, 2, value.value);
      EnumOptions.ADAPTER.encodeWithTag(writer, 3, value.options);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public EnumDescriptorProto decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.name(ProtoAdapter.STRING.decode(reader)); break;
          case 2: builder.value.add(EnumValueDescriptorProto.ADAPTER.decode(reader)); break;
          case 3: builder.options(EnumOptions.ADAPTER.decode(reader)); break;
          default: {
            FieldEncoding fieldEncoding = reader.peekFieldEncoding();
            Object value = fieldEncoding.rawProtoAdapter().decode(reader);
            builder.addUnknownField(tag, fieldEncoding, value);
          }
        }
      }
      reader.endMessage(token);
      return builder.build();
    }

    @Override
    public EnumDescriptorProto redact(EnumDescriptorProto value) {
      Builder builder = value.newBuilder();
      Internal.redactElements(builder.value, EnumValueDescriptorProto.ADAPTER);
      if (builder.options != null) builder.options = EnumOptions.ADAPTER.redact(builder.options);
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
