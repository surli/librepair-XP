/*-
 * -\-\-
 * docker-client
 * --
 * Copyright (C) 2016 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

package com.spotify.docker.client.exceptions;

public class ImagePullFailedException extends DockerException {

  private final String image;

  public ImagePullFailedException(final String image, final Throwable cause) {
    super("Image pull failed: " + image, cause);
    this.image = image;
  }

  public ImagePullFailedException(final String image, final String message) {
    super("Image pull failed: " + image + ": " + message);
    this.image = image;
  }

  public String getImage() {
    return image;
  }
}
