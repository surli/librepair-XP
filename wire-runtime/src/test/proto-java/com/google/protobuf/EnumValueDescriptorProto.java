// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: google/protobuf/descriptor.proto at 211:1
package com.google.protobuf;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import com.squareup.wire.WireField;
import com.squareup.wire.internal.Internal;
import java.io.IOException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import okio.ByteString;

/**
 * Describes a value within an enum.
 */
public final class EnumValueDescriptorProto extends Message<EnumValueDescriptorProto, EnumValueDescriptorProto.Builder> {
  public static final ProtoAdapter<EnumValueDescriptorProto> ADAPTER = new ProtoAdapter_EnumValueDescriptorProto();

  private static final long serialVersionUID = 0L;

  public static final String DEFAULT_NAME = "";

  public static final Integer DEFAULT_NUMBER = 0;

  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String name;

  @WireField(
      tag = 2,
      adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  public final Integer number;

  @WireField(
      tag = 3,
      adapter = "com.google.protobuf.EnumValueOptions#ADAPTER"
  )
  public final EnumValueOptions options;

  public EnumValueDescriptorProto(String name, Integer number, EnumValueOptions options) {
    this(name, number, options, ByteString.EMPTY);
  }

  public EnumValueDescriptorProto(String name, Integer number, EnumValueOptions options, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.name = name;
    this.number = number;
    this.options = options;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.name = name;
    builder.number = number;
    builder.options = options;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof EnumValueDescriptorProto)) return false;
    EnumValueDescriptorProto o = (EnumValueDescriptorProto) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(name, o.name)
        && Internal.equals(number, o.number)
        && Internal.equals(options, o.options);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (name != null ? name.hashCode() : 0);
      result = result * 37 + (number != null ? number.hashCode() : 0);
      result = result * 37 + (options != null ? options.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (name != null) builder.append(", name=").append(name);
    if (number != null) builder.append(", number=").append(number);
    if (options != null) builder.append(", options=").append(options);
    return builder.replace(0, 2, "EnumValueDescriptorProto{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<EnumValueDescriptorProto, Builder> {
    public String name;

    public Integer number;

    public EnumValueOptions options;

    public Builder() {
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder number(Integer number) {
      this.number = number;
      return this;
    }

    public Builder options(EnumValueOptions options) {
      this.options = options;
      return this;
    }

    @Override
    public EnumValueDescriptorProto build() {
      return new EnumValueDescriptorProto(name, number, options, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_EnumValueDescriptorProto extends ProtoAdapter<EnumValueDescriptorProto> {
    public ProtoAdapter_EnumValueDescriptorProto() {
      super(FieldEncoding.LENGTH_DELIMITED, EnumValueDescriptorProto.class);
    }

    @Override
    public int encodedSize(EnumValueDescriptorProto value) {
      return ProtoAdapter.STRING.encodedSizeWithTag(1, value.name)
          + ProtoAdapter.INT32.encodedSizeWithTag(2, value.number)
          + EnumValueOptions.ADAPTER.encodedSizeWithTag(3, value.options)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, EnumValueDescriptorProto value) throws IOException {
      ProtoAdapter.STRING.encodeWithTag(writer, 1, value.name);
      ProtoAdapter.INT32.encodeWithTag(writer, 2, value.number);
      EnumValueOptions.ADAPTER.encodeWithTag(writer, 3, value.options);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public EnumValueDescriptorProto decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.name(ProtoAdapter.STRING.decode(reader)); break;
          case 2: builder.number(ProtoAdapter.INT32.decode(reader)); break;
          case 3: builder.options(EnumValueOptions.ADAPTER.decode(reader)); break;
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
    public EnumValueDescriptorProto redact(EnumValueDescriptorProto value) {
      Builder builder = value.newBuilder();
      if (builder.options != null) builder.options = EnumValueOptions.ADAPTER.redact(builder.options);
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
