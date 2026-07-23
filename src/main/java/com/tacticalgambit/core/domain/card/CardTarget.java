package com.tacticalgambit.core.domain.card;

/**
 * Interface sellada para definir la taxonomía de objetivos de cartas de acción.
 */
public sealed interface CardTarget permits SquareTarget, PieceTarget, GlobalTarget, DoublePieceTarget {
}
