// Copyright 2023 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.google.crypto.tink.aead;

import com.google.crypto.tink.AccessesPartialKey;
import com.google.crypto.tink.Key;
import com.google.crypto.tink.util.Bytes;
import com.google.crypto.tink.util.SecretBytes;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.RestrictedApi;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Represents the Aead ChaCha20-Poly1305 specified in RFC 8439.
 *
 * <p>ChaCha20-Poly1305 allows no parameters; hence the main part here is really just the keys.
 * However, Tink allows prefixing every ciphertext with an ID-dependent prefix, see {@link
 * ChaCha20Poly1305Parameters.Variant}.
 */
@Immutable
public final class ChaCha20Poly1305Key extends AeadKey {
  private final ChaCha20Poly1305Parameters parameters;
  private final SecretBytes keyBytes;
  private final Bytes outputPrefix;
  @Nullable private final Integer idRequirement;

  private ChaCha20Poly1305Key(
      ChaCha20Poly1305Parameters parameters,
      SecretBytes keyBytes,
      Bytes outputPrefix,
      @Nullable Integer idRequirement) {
    this.parameters = parameters;
    this.keyBytes = keyBytes;
    this.outputPrefix = outputPrefix;
    this.idRequirement = idRequirement;
  }

  private static Bytes getOutputPrefix(
      ChaCha20Poly1305Parameters parameters, @Nullable Integer idRequirement) {
    if (parameters.getVariant() == ChaCha20Poly1305Parameters.Variant.NO_PREFIX) {
      return Bytes.copyFrom(new byte[] {});
    }
    if (parameters.getVariant() == ChaCha20Poly1305Parameters.Variant.CRUNCHY) {
      return Bytes.copyFrom(ByteBuffer.allocate(5).put((byte) 0).putInt(idRequirement).array());
    }
    if (parameters.getVariant() == ChaCha20Poly1305Parameters.Variant.TINK) {
      return Bytes.copyFrom(ByteBuffer.allocate(5).put((byte) 1).putInt(idRequirement).array());
    }
    throw new IllegalStateException("Unknown Variant: " + parameters.getVariant());
  }

  @Override
  public Bytes getOutputPrefix() {
    return outputPrefix;
  }

  @RestrictedApi(
      explanation = "Accessing parts of keys can produce unexpected incompatibilities, annotate the function with @AccessesPartialKey",
      link = "https://developers.google.com/tink/design/access_control#accessing_partial_keys",
      allowedOnPath = ".*Test\\.java",
      allowlistAnnotations = {AccessesPartialKey.class})
  @AccessesPartialKey
  public static ChaCha20Poly1305Key create(SecretBytes secretBytes)
      throws GeneralSecurityException {
    return create(ChaCha20Poly1305Parameters.Variant.NO_PREFIX, secretBytes, null);
  }

  @RestrictedApi(
      explanation = "Accessing parts of keys can produce unexpected incompatibilities, annotate the function with @AccessesPartialKey",
      link = "https://developers.google.com/tink/design/access_control#accessing_partial_keys",
      allowedOnPath = ".*Test\\.java",
      allowlistAnnotations = {AccessesPartialKey.class})
  public static ChaCha20Poly1305Key create(
      ChaCha20Poly1305Parameters.Variant variant,
      SecretBytes secretBytes,
      @Nullable Integer idRequirement)
      throws GeneralSecurityException {
    if (variant != ChaCha20Poly1305Parameters.Variant.NO_PREFIX && idRequirement == null) {
      throw new GeneralSecurityException(
          "For given Variant " + variant + " the value of idRequirement must be non-null");
    }
    if (variant == ChaCha20Poly1305Parameters.Variant.NO_PREFIX && idRequirement != null) {
      throw new GeneralSecurityException(
          "For given Variant NO_PREFIX the value of idRequirement must be null");
    }
    if (secretBytes.size() != 32) {
      throw new GeneralSecurityException(
          "ChaCha20Poly1305 key must be constructed with key of length 32 bytes, not "
              + secretBytes.size());
    }
    ChaCha20Poly1305Parameters parameters = ChaCha20Poly1305Parameters.create(variant);
    return new ChaCha20Poly1305Key(
        parameters, secretBytes, getOutputPrefix(parameters, idRequirement), idRequirement);
  }

  @RestrictedApi(
      explanation = "Accessing parts of keys can produce unexpected incompatibilities, annotate the function with @AccessesPartialKey",
      link = "https://developers.google.com/tink/design/access_control#accessing_partial_keys",
      allowedOnPath = ".*Test\\.java",
      allowlistAnnotations = {AccessesPartialKey.class})
  public SecretBytes getKeyBytes() {
    return keyBytes;
  }

  @Override
  public ChaCha20Poly1305Parameters getParameters() {
    return parameters;
  }

  @Override
  @Nullable
  public Integer getIdRequirementOrNull() {
    return idRequirement;
  }

  @Override
  public boolean equalsKey(Key o) {
    if (!(o instanceof ChaCha20Poly1305Key)) {
      return false;
    }
    ChaCha20Poly1305Key that = (ChaCha20Poly1305Key) o;
    // Since outputPrefix is a function of parameters, we can ignore it here.
    return that.parameters.equals(parameters)
        && that.keyBytes.equalsSecretBytes(keyBytes)
        && Objects.equals(that.idRequirement, idRequirement);
  }
}
