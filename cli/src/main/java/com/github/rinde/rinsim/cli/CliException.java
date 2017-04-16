/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.cli;

import static com.google.common.base.Preconditions.checkState;

import javax.annotation.Nullable;

import com.google.common.base.Optional;

/**
 * Exception indicating a problem with the command-line interface.
 * @author Rinde van Lon
 */
public class CliException extends RuntimeException {
  private static final long serialVersionUID = -7434606684541234080L;
  private final Optional<Option> menuOption;
  private final CauseType causeType;

  CliException(String msg, CauseType type, @Nullable Option opt) {
    this(msg, null, type, opt);
  }

  CliException(String msg, @Nullable Throwable cause, CauseType type,
      @Nullable Option opt) {
    super(msg, cause);
    menuOption = Optional.<Option>fromNullable(opt);
    causeType = type;
  }

  /**
   * @return The {@link Option} where the exception occurred, or
   *         {@link Optional#absent()} if there is no {@link Option} responsible
   *         for this exception.
   */
  public Optional<Option> getMenuOption() {
    checkState(menuOption.isPresent(), "'%s' has no reference to an option.",
      toString());
    return menuOption;
  }

  static void checkAlreadySelected(boolean notSelected, Option opt,
      String format, Object... args) {
    if (!notSelected) {
      throw new CliException(String.format(format, args),
        CauseType.ALREADY_SELECTED, opt);
    }
  }

  static void checkCommand(boolean recognized, String format, Object... args) {
    check(recognized, CauseType.UNRECOGNIZED_COMMAND, null, format, args);
  }

  static void checkArgFormat(boolean correct, Option opt, String format,
      Object... args) {
    check(correct, CauseType.INVALID_ARG_FORMAT, opt, format, args);
  }

  static void check(boolean condition, CauseType cause, @Nullable Option opt,
      String format, Object... args) {
    if (!condition) {
      throw new CliException(String.format(format, args), cause, opt);
    }
  }

  /**
   * @return The {@link CauseType} of this exception.
   */
  public CauseType getCauseType() {
    return causeType;
  }

  /**
   * Collection of causes of command-line interface exceptions.
   * @author Rinde van Lon
   */
  public enum CauseType {
    /**
     * An argument of an option is missing which should have been defined.
     */
    MISSING_ARG,

    /**
     * An argument of an option was found where none was expected.
     */
    UNEXPECTED_ARG,

    /**
     * This option has already been selected.
     */
    ALREADY_SELECTED,

    /**
     * An unrecognized command is found.
     */
    UNRECOGNIZED_COMMAND,

    /**
     * The argument format is invalid.
     */
    INVALID_ARG_FORMAT,

    /**
     * An error has occurred during execution of the {@link ArgHandler}.
     */
    HANDLER_FAILURE;
  }
}
