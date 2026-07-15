package com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.authenticate;

import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * GA-14624: gameId is optional in PP authenticate (spec §3.4). PP omits it when the player
 * re-authenticates with an active token (e.g. re-entry after Feature Buy); GA used to reject
 * such requests with error 7 and the player could not enter the game.
 */
class AuthenticateDtoValidationTest {

    // Actual request body from the GA-14624 incident (no gameId parameter)
    private static final String BODY_WITHOUT_GAME_ID =
            "hash=331c8670d7a2e305c6b69a9d241b2d2b&providerId=PragmaticPlay&token=cfb34ee2-715c-44af-9a5c-80d2d4b6f45d&userId=e5jefvrxl";

    private static final String BODY_WITH_GAME_ID = BODY_WITHOUT_GAME_ID + "&gameId=104";

    @Test
    void authenticateWithoutGameIdPassesValidation() throws InvalidRequestException {
        AuthenticateDto dto = HttpService.convertQueryStringToDto(BODY_WITHOUT_GAME_ID, AuthenticateDto.class);

        assertThat(dto.getGameId()).isNull();
        assertThatCode(() -> ValidationUtils.validateRequest(dto)).doesNotThrowAnyException();
    }

    @Test
    void authenticateWithGameIdPassesValidation() throws InvalidRequestException {
        AuthenticateDto dto = HttpService.convertQueryStringToDto(BODY_WITH_GAME_ID, AuthenticateDto.class);

        assertThat(dto.getGameId()).isEqualTo("104");
        assertThatCode(() -> ValidationUtils.validateRequest(dto)).doesNotThrowAnyException();
    }

    @Test
    void malformedGameIdIsStillRejected() throws InvalidRequestException {
        AuthenticateDto dto = HttpService.convertQueryStringToDto(BODY_WITHOUT_GAME_ID, AuthenticateDto.class);
        dto.setGameId("104$;drop");

        assertThatExceptionOfType(InvalidRequestException.class)
                .isThrownBy(() -> ValidationUtils.validateRequest(dto));
    }

    @Test
    void missingTokenIsStillRejected() throws InvalidRequestException {
        AuthenticateDto dto = HttpService.convertQueryStringToDto(
                "hash=331c8670d7a2e305c6b69a9d241b2d2b&providerId=PragmaticPlay&gameId=104", AuthenticateDto.class);

        assertThatExceptionOfType(InvalidRequestException.class)
                .isThrownBy(() -> ValidationUtils.validateRequest(dto));
    }

    @Test
    void legacyDtoAcceptsMissingGameIdAsWell() throws InvalidRequestException {
        com.nextgen.gameaggregator.vendor.pragmaticplay.api.authenticate.AuthenticateDto dto =
                HttpService.convertQueryStringToDto(BODY_WITHOUT_GAME_ID,
                        com.nextgen.gameaggregator.vendor.pragmaticplay.api.authenticate.AuthenticateDto.class);

        assertThat(dto.getGameId()).isNull();
        assertThatCode(() -> ValidationUtils.validateRequest(dto)).doesNotThrowAnyException();
    }
}
