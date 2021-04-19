/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.card.generic;

/**
 * Exception when an error or a communication failure with the card or the reader occurs.
 *
 * @since 2.0
 */
public class TransactionException extends RuntimeException {

  /**
   * Builds a new exception.
   *
   * @param message Message to identify the exception context.
   * @since 2.0
   */
  public TransactionException(String message) {
    super(message);
  }

  /**
   * Builds a new exception with the originating exception.
   *
   * @param message Message to identify the exception context.
   * @param cause The cause
   * @since 2.0
   */
  public TransactionException(String message, Throwable cause) {
    super(message, cause);
  }
}
