package com.oneonline.backend.service.game;

import com.oneonline.backend.model.domain.*;
import com.oneonline.backend.model.enums.CardColor;
import com.oneonline.backend.model.enums.CardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CardValidator
 *
 * Tests card validation logic according to ONE game rules.
 */
@DisplayName("Card Validator Tests")
class CardValidatorTest {

    private CardValidator cardValidator;

    @BeforeEach
    void setUp() {
        cardValidator = new CardValidator();
    }

    // ========================================
    // NUMBER CARD VALIDATION TESTS
    // ========================================

    @Test
    @DisplayName("Should allow number card with same color")
    void shouldAllowNumberCardWithSameColor() {
        // Given
        Card topCard = new NumberCard(CardColor.RED, 5);
        Card cardToPlay = new NumberCard(CardColor.RED, 3);

        // When
        boolean isValid = cardValidator.isValidMove(cardToPlay, topCard);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should allow number card with same number")
    void shouldAllowNumberCardWithSameNumber() {
        // Given
        Card topCard = new NumberCard(CardColor.RED, 5);
        Card cardToPlay = new NumberCard(CardColor.BLUE, 5);

        // When
        boolean isValid = cardValidator.isValidMove(cardToPlay, topCard);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject number card with different color and number")
    void shouldRejectNumberCardWithDifferentColorAndNumber() {
        // Given
        Card topCard = new NumberCard(CardColor.RED, 5);
        Card cardToPlay = new NumberCard(CardColor.BLUE, 3);

        // When
        boolean isValid = cardValidator.isValidMove(cardToPlay, topCard);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should allow number card on action card with same color")
    void shouldAllowNumberCardOnActionCardWithSameColor() {
        // Given
        Card topCard = new SkipCard(CardColor.RED);
        Card cardToPlay = new NumberCard(CardColor.RED, 5);

        // When
        boolean isValid = cardValidator.isValidMove(cardToPlay, topCard);

        // Then
        assertThat(isValid).isTrue();
    }

    // ========================================
    // ACTION CARD VALIDATION TESTS
    // ========================================

    @Test
    @DisplayName("Should allow Skip card with same color")
    void shouldAllowSkipCardWithSameColor() {
        // Given
        Card topCard = new NumberCard(CardColor.RED, 5);
        Card cardToPlay = new SkipCard(CardColor.RED);

        // When
        boolean isValid = cardValidator.canPlayActionCard(cardToPlay, topCard);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should allow Skip card on Skip card")
    void shouldAllowSkipCardOnSkipCard() {
        // Given
        Card topCard = new SkipCard(CardColor.RED);
        Card cardToPlay = new SkipCard(CardColor.BLUE);

        // When
        boolean isValid = cardValidator.canPlayActionCard(cardToPlay, topCard);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should allow Reverse card with same color")
    void shouldAllowReverseCardWithSameColor() {
        // Given
        Card topCard = new NumberCard(CardColor.GREEN, 3);
        Card cardToPlay = new ReverseCard(CardColor.GREEN);

        // When
        boolean isValid = cardValidator.canPlayActionCard(cardToPlay, topCard);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should allow Reverse card on Reverse card")
    void shouldAllowReverseCardOnReverseCard() {
        // Given
        Card topCard = new ReverseCard(CardColor.GREEN);
        Card cardToPlay = new ReverseCard(CardColor.YELLOW);

        // When
        boolean isValid = cardValidator.canPlayActionCard(cardToPlay, topCard);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should allow Draw Two card with same color")
    void shouldAllowDrawTwoCardWithSameColor() {
        // Given
        Card topCard = new NumberCard(CardColor.BLUE, 7);
        Card cardToPlay = new DrawTwoCard(CardColor.BLUE);

        // When
        boolean isValid = cardValidator.canPlayActionCard(cardToPlay, topCard);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should allow Draw Two card on Draw Two card")
    void shouldAllowDrawTwoCardOnDrawTwoCard() {
        // Given
        Card topCard = new DrawTwoCard(CardColor.BLUE);
        Card cardToPlay = new DrawTwoCard(CardColor.RED);

        // When
        boolean isValid = cardValidator.canPlayActionCard(cardToPlay, topCard);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject action card with different color and type")
    void shouldRejectActionCardWithDifferentColorAndType() {
        // Given
        Card topCard = new SkipCard(CardColor.RED);
        Card cardToPlay = new ReverseCard(CardColor.BLUE);

        // When
        boolean isValid = cardValidator.canPlayActionCard(cardToPlay, topCard);

        // Then
        assertThat(isValid).isFalse();
    }

    // ========================================
    // WILD CARD VALIDATION TESTS
    // ========================================

    @Test
    @DisplayName("Should always allow Wild card")
    void shouldAlwaysAllowWildCard() {
        // Given
        Card topCard = new NumberCard(CardColor.RED, 5);
        Card cardToPlay = new WildCard();

        // When
        boolean isValid = cardValidator.canPlayWildCard((WildCard) cardToPlay, topCard);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should allow Wild card on action card")
    void shouldAllowWildCardOnActionCard() {
        // Given
        Card topCard = new SkipCard(CardColor.GREEN);
        Card cardToPlay = new WildCard();

        // When
        boolean isValid = cardValidator.canPlayWildCard((WildCard) cardToPlay, topCard);

        // Then
        assertThat(isValid).isTrue();
    }

    // ========================================
    // WILD DRAW FOUR VALIDATION TESTS
    // ========================================

    @Test
    @DisplayName("Should allow Wild Draw Four when no other valid cards")
    void shouldAllowWildDrawFourWhenNoOtherValidCards() {
        // Given
        Card topCard = new NumberCard(CardColor.RED, 5);
        WildDrawFourCard wildDrawFour = new WildDrawFourCard();
        List<Card> hand = Arrays.asList(
            new NumberCard(CardColor.BLUE, 3),
            new NumberCard(CardColor.GREEN, 7),
            wildDrawFour
        );

        // When
        boolean isLegal = cardValidator.canPlayWildDrawFourLegally(wildDrawFour, topCard, hand);

        // Then
        assertThat(isLegal).isTrue();
    }

    @Test
    @DisplayName("Should reject Wild Draw Four when valid cards exist")
    void shouldRejectWildDrawFourWhenValidCardsExist() {
        // Given
        Card topCard = new NumberCard(CardColor.RED, 5);
        WildDrawFourCard wildDrawFour = new WildDrawFourCard();
        List<Card> hand = Arrays.asList(
            new NumberCard(CardColor.RED, 3),  // Valid card!
            new NumberCard(CardColor.BLUE, 7),
            wildDrawFour
        );

        // When
        boolean isLegal = cardValidator.canPlayWildDrawFourLegally(wildDrawFour, topCard, hand);

        // Then
        assertThat(isLegal).isFalse();
    }

    @Test
    @DisplayName("Should allow Wild Draw Four when only wild cards in hand")
    void shouldAllowWildDrawFourWhenOnlyWildCards() {
        // Given
        Card topCard = new NumberCard(CardColor.RED, 5);
        WildDrawFourCard wildDrawFour = new WildDrawFourCard();
        List<Card> hand = Arrays.asList(
            new WildCard(),
            wildDrawFour
        );

        // When
        boolean isLegal = cardValidator.canPlayWildDrawFourLegally(wildDrawFour, topCard, hand);

        // Then
        assertThat(isLegal).isTrue();
    }

    // ========================================
    // VALID CARDS SEARCH TESTS
    // ========================================

    @Test
    @DisplayName("Should find all valid cards in hand")
    void shouldFindAllValidCardsInHand() {
        // Given
        Card topCard = new NumberCard(CardColor.RED, 5);
        List<Card> hand = Arrays.asList(
            new NumberCard(CardColor.RED, 3),    // Valid (same color)
            new NumberCard(CardColor.BLUE, 5),   // Valid (same number)
            new NumberCard(CardColor.GREEN, 2),  // Invalid
            new SkipCard(CardColor.RED),         // Valid (same color)
            new ReverseCard(CardColor.BLUE),     // Invalid
            new WildCard()                       // Valid (wild)
        );

        // When
        List<Card> validCards = cardValidator.getValidCards(hand, topCard);

        // Then
        assertThat(validCards).hasSize(4);
    }

    @Test
    @DisplayName("Should detect if player has valid card")
    void shouldDetectIfPlayerHasValidCard() {
        // Given
        Card topCard = new NumberCard(CardColor.RED, 5);
        List<Card> hand = Arrays.asList(
            new NumberCard(CardColor.RED, 3),
            new NumberCard(CardColor.BLUE, 7)
        );

        // When
        boolean hasValid = cardValidator.hasValidCard(hand, topCard);

        // Then
        assertThat(hasValid).isTrue();
    }

    @Test
    @DisplayName("Should detect when player has no valid cards")
    void shouldDetectWhenPlayerHasNoValidCards() {
        // Given
        Card topCard = new NumberCard(CardColor.RED, 5);
        List<Card> hand = Arrays.asList(
            new NumberCard(CardColor.BLUE, 3),
            new NumberCard(CardColor.GREEN, 7)
        );

        // When
        boolean hasValid = cardValidator.hasValidCard(hand, topCard);

        // Then
        assertThat(hasValid).isFalse();
    }

    // ========================================
    // COLOR/NUMBER/TYPE MATCHING TESTS
    // ========================================

    @Test
    @DisplayName("Should validate color choice for Wild card")
    void shouldValidateColorChoiceForWildCard() {
        // Valid choices
        assertThat(cardValidator.isValidColorChoice(CardColor.RED)).isTrue();
        assertThat(cardValidator.isValidColorChoice(CardColor.BLUE)).isTrue();
        assertThat(cardValidator.isValidColorChoice(CardColor.GREEN)).isTrue();
        assertThat(cardValidator.isValidColorChoice(CardColor.YELLOW)).isTrue();

        // Invalid choices
        assertThat(cardValidator.isValidColorChoice(CardColor.WILD)).isFalse();
        assertThat(cardValidator.isValidColorChoice(null)).isFalse();
    }

    @Test
    @DisplayName("Should match card by color")
    void shouldMatchCardByColor() {
        // Given
        Card redCard = new NumberCard(CardColor.RED, 5);
        Card wildCard = new WildCard();

        // Then
        assertThat(cardValidator.matchesColor(redCard, CardColor.RED)).isTrue();
        assertThat(cardValidator.matchesColor(redCard, CardColor.BLUE)).isFalse();
        assertThat(cardValidator.matchesColor(wildCard, CardColor.RED)).isTrue(); // Wild matches any
    }

    @Test
    @DisplayName("Should match card by number")
    void shouldMatchCardByNumber() {
        // Given
        Card numberCard = new NumberCard(CardColor.RED, 5);
        Card skipCard = new SkipCard(CardColor.RED);

        // Then
        assertThat(cardValidator.matchesNumber(numberCard, 5)).isTrue();
        assertThat(cardValidator.matchesNumber(numberCard, 3)).isFalse();
        assertThat(cardValidator.matchesNumber(skipCard, 5)).isFalse(); // Not a number card
    }

    @Test
    @DisplayName("Should match card by type")
    void shouldMatchCardByType() {
        // Given
        Card skipCard = new SkipCard(CardColor.RED);
        Card numberCard = new NumberCard(CardColor.RED, 5);

        // Then
        assertThat(cardValidator.matchesType(skipCard, CardType.SKIP)).isTrue();
        assertThat(cardValidator.matchesType(skipCard, CardType.REVERSE)).isFalse();
        assertThat(cardValidator.matchesType(numberCard, CardType.NUMBER)).isTrue();
    }

    // ========================================
    // MUST DRAW VALIDATION TESTS
    // ========================================

    @Test
    @DisplayName("Should determine player must draw when no valid cards")
    void shouldDeterminePlayerMustDrawWhenNoValidCards() {
        // Given
        Card topCard = new NumberCard(CardColor.RED, 5);
        List<Card> hand = Arrays.asList(
            new NumberCard(CardColor.BLUE, 3),
            new NumberCard(CardColor.GREEN, 7)
        );

        // When
        boolean mustDraw = cardValidator.mustDrawCard(hand, topCard);

        // Then
        assertThat(mustDraw).isTrue();
    }

    @Test
    @DisplayName("Should determine player doesn't need to draw when valid cards exist")
    void shouldDeterminePlayerDoesntNeedToDrawWhenValidCardsExist() {
        // Given
        Card topCard = new NumberCard(CardColor.RED, 5);
        List<Card> hand = Arrays.asList(
            new NumberCard(CardColor.RED, 3),  // Valid
            new NumberCard(CardColor.BLUE, 7)
        );

        // When
        boolean mustDraw = cardValidator.mustDrawCard(hand, topCard);

        // Then
        assertThat(mustDraw).isFalse();
    }

    // ========================================
    // COUNT VALID PLAYS TESTS
    // ========================================

    @Test
    @DisplayName("Should count number of valid plays")
    void shouldCountNumberOfValidPlays() {
        // Given
        Card topCard = new NumberCard(CardColor.RED, 5);
        List<Card> hand = Arrays.asList(
            new NumberCard(CardColor.RED, 3),    // Valid
            new NumberCard(CardColor.BLUE, 5),   // Valid
            new NumberCard(CardColor.GREEN, 2),  // Invalid
            new SkipCard(CardColor.RED),         // Valid
            new ReverseCard(CardColor.BLUE)      // Invalid
        );

        // When
        int count = cardValidator.countValidPlays(hand, topCard);

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Should count zero when no valid plays")
    void shouldCountZeroWhenNoValidPlays() {
        // Given
        Card topCard = new NumberCard(CardColor.RED, 5);
        List<Card> hand = Arrays.asList(
            new NumberCard(CardColor.BLUE, 3),
            new NumberCard(CardColor.GREEN, 7)
        );

        // When
        int count = cardValidator.countValidPlays(hand, topCard);

        // Then
        assertThat(count).isZero();
    }
}
