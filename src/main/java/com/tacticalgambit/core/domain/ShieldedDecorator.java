package com.tacticalgambit.core.domain;

/**
 * Decorador de pieza que la protege contra capturas enemigas (Escudo).
 */
public final class ShieldedDecorator extends PieceDecorator {

    public ShieldedDecorator(Piece delegate) {
        super(delegate);
    }
}
