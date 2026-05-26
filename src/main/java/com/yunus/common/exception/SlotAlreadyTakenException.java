package com.yunus.common.exception;

/**
 * Randevu slotu başka bir kullanıcı tarafından alınmış olduğunda fırlatılan exception.
 * HTTP 409 Conflict olarak döner.
 */
public class SlotAlreadyTakenException extends RuntimeException {

    public SlotAlreadyTakenException(String message) {
        super(message);
    }
}
