package ch.sbb.polarion.extension.generic.rest.exception;

import ch.sbb.polarion.extension.generic.rest.filter.AuthenticationFilter;

import javax.ws.rs.NotAuthorizedException;

/**
 * Custom exception representing an unauthorized access attempt.
 * Extends the NotAuthorizedException from JAX-RS which means it will be handled
 * by {@link ch.sbb.polarion.extension.generic.rest.exception.mapper.NotAuthorizedExceptionMapper}.
 */
@SuppressWarnings("squid:S110")
public class UnauthorizedException extends NotAuthorizedException {

    public UnauthorizedException(String message) {
        super(message, AuthenticationFilter.BEARER);
    }

}
