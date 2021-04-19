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

import org.eclipse.keyple.core.card.CardApiProperties;
import org.eclipse.keyple.core.card.spi.CardExtensionSpi;
import org.eclipse.keyple.core.common.CommonsApiProperties;
import org.eclipse.keyple.core.service.CardResource;
import org.eclipse.keyple.core.service.ServiceApiProperties;
import org.eclipse.keyple.core.service.selection.CardSelector;
import org.eclipse.keyple.core.service.selection.spi.CardSelection;

/**
 * Implementation of {@link GenericCardExtension}.
 *
 * @since 2.0
 */
final class GenericCardExtensionAdapter implements GenericCardExtension, CardExtensionSpi {

  private static final GenericCardExtensionAdapter instance = new GenericCardExtensionAdapter();

  /** (package-private)<br> */
  GenericCardExtensionAdapter() {}

  /**
   * (package-private)<br>
   * Gets the unique instance of this object.
   *
   * @return A not null reference.
   */
  static GenericCardExtension getInstance() {
    return instance;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public CardSelection createGenericCardSelection(CardSelector cardSelector) {
    return new GenericCardSelectionAdapter(cardSelector);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public GenericCardTransaction createGenericCardTransaction(CardResource cardResource) {
    return new GenericCardTransactionAdapter(cardResource);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public String getCardApiVersion() {
    return CardApiProperties.VERSION;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public String getServiceApiVersion() {
    return ServiceApiProperties.VERSION;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public String getCommonsApiVersion() {
    return CommonsApiProperties.VERSION;
  }
}
