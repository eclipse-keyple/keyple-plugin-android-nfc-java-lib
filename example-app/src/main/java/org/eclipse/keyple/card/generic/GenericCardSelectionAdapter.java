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

import org.eclipse.keyple.core.card.CardSelectionRequest;
import org.eclipse.keyple.core.card.CardSelectionResponse;
import org.eclipse.keyple.core.card.spi.CardSelectionSpi;
import org.eclipse.keyple.core.card.spi.SmartCardSpi;
import org.eclipse.keyple.core.service.selection.CardSelector;
import org.eclipse.keyple.core.service.selection.spi.CardSelection;

/**
 * Implementation of a generic {@link CardSelection}.
 *
 * @since 2.0
 */
class GenericCardSelectionAdapter implements CardSelection, CardSelectionSpi {

  /** (private) */
  private final CardSelector cardSelector;

  /**
   * (package-private) Creates an instance.
   *
   * @param cardSelector A card selector.
   */
  GenericCardSelectionAdapter(CardSelector cardSelector) {
    this.cardSelector = cardSelector;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public CardSelector getCardSelector() {
    return cardSelector;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public CardSelectionRequest getCardSelectionRequest() {
    return new CardSelectionRequest(cardSelector);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public SmartCardSpi parse(CardSelectionResponse cardSelectionResponse) {
    return new GenericSmartCardAdapter(cardSelectionResponse);
  }
}
