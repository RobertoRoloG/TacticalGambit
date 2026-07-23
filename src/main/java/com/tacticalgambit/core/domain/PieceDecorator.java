package com.tacticalgambit.core.domain;

/**
 * Decorador abstracto para envolver piezas y aplicar efectos especiales.
 */
public abstract sealed class PieceDecorator implements Piece permits ShieldedDecorator, JumpModifierDecorator {
    protected final Piece delegate;

    protected PieceDecorator(Piece delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("La pieza delegada no puede ser nula.");
        }
        this.delegate = delegate;
    }

    public Piece delegate() {
        return delegate;
    }

    @Override
    public PieceColor color() {
        return delegate.color();
    }

    @Override
    public PieceType type() {
        return delegate.type();
    }

    @Override
    public boolean canMove(Board board, Square from, Square to) {
        return delegate.canMove(board, from, to);
    }

    /**
     * Devuelve la pieza base desempaquetando todos los decoradores.
     */
    public static Piece basePiece(Piece piece) {
        if (piece instanceof PieceDecorator decorator) {
            return basePiece(decorator.delegate());
        }
        return piece;
    }

    /**
     * Verifica si una pieza está protegida con escudo.
     */
    public static boolean isShielded(Piece piece) {
        if (piece instanceof ShieldedDecorator) {
            return true;
        }
        if (piece instanceof PieceDecorator decorator) {
            return isShielded(decorator.delegate());
        }
        return false;
    }

    /**
     * Verifica si una pieza tiene activo el modificador de salto táctico.
     */
    public static boolean hasJumpModifier(Piece piece) {
        if (piece instanceof JumpModifierDecorator) {
            return true;
        }
        if (piece instanceof PieceDecorator decorator) {
            return hasJumpModifier(decorator.delegate());
        }
        return false;
    }

    /**
     * Desempaqueta y remueve un escudo (solución limpia sin clases anónimas).
     */
    public static Piece cleanShield(Piece piece) {
        if (piece instanceof ShieldedDecorator shielded) {
            return cleanShield(shielded.delegate());
        }
        if (piece instanceof JumpModifierDecorator jump) {
            return new JumpModifierDecorator(cleanShield(jump.delegate()));
        }
        return piece;
    }

    /**
     * Remueve el decorador de salto táctico de la pieza si existe.
     */
    public static Piece cleanTacticalJump(Piece piece) {
        if (piece instanceof JumpModifierDecorator jump) {
            return cleanTacticalJump(jump.delegate());
        }
        if (piece instanceof ShieldedDecorator shielded) {
            return new ShieldedDecorator(cleanTacticalJump(shielded.delegate()));
        }
        return piece;
    }
}
