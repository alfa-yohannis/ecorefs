package org.ecorefs.eval.baselines;

/**
 * What a visit of a model saw: the number of elements and the sum of their
 * identifiers. Every iteration stamps its own number into the root, so a store
 * that returns the content of an earlier iteration fails the comparison even
 * when the element count matches.
 *
 * @param elements      the number of visited elements
 * @param identifierSum the sum of the identifiers of the visited elements
 */
public record VisitSummary(long elements, long identifierSum) {
}
