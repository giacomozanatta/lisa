package it.unive.lisa.analysis.nonRedundantSet;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import java.util.HashSet;
import java.util.Set;

/**
 * This abstract class generalize the concept of an abstract domain whose domain
 * is the finite non redundant subset of the domain of another lattice (in this
 * case the other lattice is of type E). This implementation follows the
 * guidelines of this
 * <a href="https://www.cs.unipr.it/Publications/PDF/Q349.pdf">paper</a>. It is
 * implemented as a {@link BaseNonRelationalValueDomain}, handling top and
 * bottom values for the expression evaluation and bottom values for the
 * expression satisfiability. Top and bottom cases for least upper bounds,
 * widening and less or equals operations are handled by {@link BaseLattice} in
 * {@link BaseLattice#lub}, {@link BaseLattice#widening} and
 * {@link BaseLattice#lessOrEqual} methods, respectively.
 * 
 * @param <C> the concrete {@link NonRedundantPowerset} instance
 * @param <E> the type of the elements contained in this set
 */
public abstract class NonRedundantPowersetOfBaseNonRelationalValueDomain<
		C extends NonRedundantPowersetOfBaseNonRelationalValueDomain<C, E>,
		E extends BaseNonRelationalValueDomain<E>> implements BaseNonRelationalValueDomain<C> {

	/**
	 * The set that containing the elements.
	 */
	protected final Set<E> elementsSet;

	/**
	 * The underlying {@link BaseNonRelationalValueDomain} by which it can be
	 * possible to retrieve top and bottom elements.
	 */
	protected final E valueDomain;

	/**
	 * Creates an instance with elementsSet as elements and valueDomain as
	 * element.
	 * 
	 * @param elements the set of elements in the set
	 * @param element  the underlying {@link BaseNonRelationalValueDomain}
	 */
	protected NonRedundantPowersetOfBaseNonRelationalValueDomain(Set<E> elements, E element) {
		elementsSet = new HashSet<E>(elements);
		valueDomain = element.bottom();
	}

	/**
	 * Utility for creating a concrete instance of
	 * {@link NonRedundantPowersetOfBaseNonRelationalValueDomain} given a set.
	 * 
	 * @param elements the set containing the elements that must be included in
	 *                     the lattice instance
	 * 
	 * @return a new concrete instance of {@link NonRedundantPowerset}
	 *             containing the elements of the given set
	 */
	protected abstract C mk(Set<E> elements);

	/**
	 * Yields a new concrete set of elements equivalent to this but that is not
	 * redundant. An element x of a subset S of the domain of a lattice is
	 * redundant iff exists another element y in S such that x &le; y. Given a
	 * redundant set is always possible to construct an equivalent set that is
	 * not redundant. This operator is usually called omega reduction
	 * (represented as &#937;). Given a subset S of a domain of a lattice:
	 * &#937;(S) = S \ {s &ni; S | ( s = bottom ) OR ( &exist; s' &ni; S. s
	 * &le; s' )}
	 *
	 * @return an equivalent element that is not redundant.
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	protected C removeRedundancy() throws SemanticException {
		Set<E> newElementsSet = new HashSet<E>();
		for (E element : elementsSet) {
			if (!element.isBottom()) {
				boolean toRemove = false;
				for (E otherElement : elementsSet)
					if (element.lessOrEqual(otherElement) && !otherElement.lessOrEqual(element))
						toRemove = true;
				if (!toRemove)
					newElementsSet.add(element);
			}
		}
		return mk(newElementsSet);
	}

	/**
	 * Yields a new concrete set less or equal to this that has not overlapping
	 * elements inside. Two elements of a set are overlapping iff their glb is
	 * not bottom. It uses an auxiliary method
	 * removeOverlappingBetweenElelements that knows how to remove the
	 * overlapping between two elements of type E.
	 * 
	 * @return an equivalent set of elements that doesn't have overlapping
	 *             elements.
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	protected C removeOverlapping() throws SemanticException {

		Set<E> newSet;
		Set<E> tmpSet = new HashSet<E>(this.elementsSet);
		do {
			newSet = new HashSet<E>(tmpSet);
			tmpSet = new HashSet<E>();
			for (E e1 : newSet) {
				boolean intersectionFound = false;
				for (E e2 : newSet) {
					if (!e1.glb(e2).isBottom()) {
						E notOverlappingElement = removeOverlappingBetweenElements(e1, e2);
						if (!tmpSet.contains(notOverlappingElement))
							tmpSet.add(notOverlappingElement);
						intersectionFound = true;
					}
				}
				if (!intersectionFound)
					tmpSet.add(e1);
			}
		} while (tmpSet.size() != newSet.size());
		return mk(tmpSet).removeRedundancy();
	}

	/**
	 * Yields a new element equivalent to the two overlapping elements e1 and
	 * e2. By default it calculates the lub between the two elements. But if
	 * exist a better method that looses less information it can be implemented
	 * in the concrete class.
	 * 
	 * @param e1 the first element
	 * @param e2 the second element
	 * 
	 * @return an element equivalent to the two overlapping elements e1 and e2
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	protected E removeOverlappingBetweenElements(E e1, E e2) throws SemanticException {
		return e1.lub(e2);
	}

	/**
	 * An Egli-Milner connector is an upper bound operator for the
	 * {@link #lessOrEqualEgliMilner(NonRedundantPowersetOfBaseNonRelationalValueDomain)
	 * Egli-Milner relation &le;<sub>EM</sub>}. An Egli-Milner connector is
	 * represented as &boxplus;<sub>EM</sub>. Given two subsets S<sub>1</sub>
	 * and S<sub>2</sub> of a domain of a lattice S<sub>1</sub>
	 * &boxplus;<sub>EM</sub> S<sub>2</sub> = S<sub>3</sub> such that (
	 * S<sub>1</sub> &le;<sub>EM</sub> S<sub>3</sub> ) AND ( S<sub>1</sub>
	 * &le;<sub>EM</sub> S<sub>3</sub> ). The default implementation just
	 * performs the lub on the union of the two sets.
	 * 
	 * @param other the other concrete element
	 * 
	 * @return a new set that is &le;<sub>EM</sub> than both this and other
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	protected C EgliMilnerConnector(C other) throws SemanticException {
		Set<E> unionSet = new HashSet<E>(elementsSet);
		unionSet.addAll(other.elementsSet);
		E completeLub = valueDomain.bottom();
		if (!unionSet.isEmpty()) {
			for (E element : unionSet) {
				completeLub = completeLub.lub(element);
			}
		}
		Set<E> newSet = new HashSet<E>();
		if (completeLub != null)
			newSet.add(completeLub);
		return mk(newSet);
	}

	@Override
	public DomainRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();

		String representation = "[";
		boolean first = true;

		for (E element : this.elementsSet) {
			if (!first)
				representation += ", ";
			else
				first = false;

			representation += element.representation();
		}

		representation += "]";

		return new StringRepresentation(representation);
	}

	/**
	 * Add an element to the set.
	 * 
	 * @param element the element that is added to the set
	 */
	protected void add(E element) {
		elementsSet.add(element);
	};

	/**
	 * Performs the least upper bound between this non redundant set and the
	 * given one. The lub between two non redundant sets is the union of the two
	 * removed of redundancy.
	 */
	@Override
	public C lubAux(C other)
			throws SemanticException {
		Set<E> lubSet = new HashSet<E>(elementsSet);
		lubSet.addAll(other.elementsSet);
		return mk(lubSet).removeRedundancy().removeOverlapping();
	}

	@Override
	public C glbAux(C other)
			throws SemanticException {
		Set<E> glbSet = new HashSet<E>();
		for (E s1 : elementsSet)
			for (E s2 : other.elementsSet)
				glbSet.add(s1.glb(s2));
		return mk(glbSet).removeRedundancy().removeOverlapping();
	}

	/**
	 * This method implements the widening-connected hextrapolation heuristic
	 * proposed in the
	 * <a href="https://www.cs.unipr.it/Publications/PDF/Q349.pdf">paper</a>
	 * (represented as h<sup>&nabla;</sup>). Given two subsets S<sub>1</sub> and
	 * S<sub>2</sub> of a domain of a lattice:
	 * <p>
	 * h<sup>&nabla;</sup>( S<sub>1</sub>, S<sub>2</sub>) = S<sub>2</sub>
	 * &sqcup; &#937;({ s<sub>1</sub> &nabla; s<sub>2</sub> | s<sub>1</sub> &ni;
	 * S<sub>1</sub>, s<sub>2</sub> &ni; S<sub>2</sub>, s<sub>1</sub> &lt;
	 * s<sub>2</sub>})
	 * </p>
	 * where
	 * <ul>
	 * <li>&#937; is the {@link #removeRedundancy() omega reduction} operator
	 * that removes redundancy from a set,</li>
	 * <li>&nabla; is the widening operator of the underlying lattice,</li>
	 * <li>&lt; is the strict partial order relation of the underlying
	 * lattice,</li>
	 * <li>&sqcup; is the
	 * {@link #lubAux(NonRedundantPowersetOfBaseNonRelationalValueDomain) least
	 * upper bound} operator between non redundant subsets of the domain of the
	 * underlying lattice.</li>
	 * </ul>
	 * 
	 * @param other the other set to perform the extrapolation with
	 * 
	 * @return the new extrapolated set
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	protected C extrapolationHeuristic(C other) throws SemanticException {
		Set<E> extrapolatedSet = new HashSet<E>();
		for (E s1 : elementsSet) {
			for (E s2 : other.elementsSet) {
				if (s1.lessOrEqual(s2) && !s2.lessOrEqual(s1))
					extrapolatedSet.add(s1.widening(s2));
			}
		}
		return mk(extrapolatedSet).removeRedundancy().lub(other);
	}

	/**
	 * Perform the wideninig operation between two finite non redundant subsets
	 * of the domain of a lattice following the Egli-Milner widening
	 * implementation shown in this
	 * <a href="https://www.cs.unipr.it/Publications/PDF/Q349.pdf">paper</a>.
	 * Given two subset S<sub>1</sub> and S<sub>2</sub> of the domain of a
	 * lattice widening(S<sub>1</sub>, S<sub>2</sub>) =
	 * h<sup>&nabla;</sup>(S<sub>1</sub>, T<sub>2</sub>), where
	 * h<sup>&nabla;</sup> is a
	 * {@link #extrapolationHeuristic(NonRedundantPowersetOfBaseNonRelationalValueDomain)
	 * widenining-connected extrapolation heuristic} and T<sub>2</sub> is equal
	 * to:
	 * <ul>
	 * <li>S<sub>2</sub> &ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp; if
	 * S<sub>1</sub> &le;<sub>EM</sub> S<sub>2</sub></li>
	 * <li>S<sub>1</sub> &boxplus;<sub>EM</sub> S<sub>2</sub> &ensp;
	 * otherwise</li>
	 * </ul>
	 * where &le;<sub>EM</sub> is the
	 * {@link #lessOrEqualEgliMilner(NonRedundantPowersetOfBaseNonRelationalValueDomain)
	 * Egli-Milner relation} and &boxplus;<sub>EM</sub> is an
	 * {@link #EgliMilnerConnector(NonRedundantPowersetOfBaseNonRelationalValueDomain)
	 * Egli-Milner connector}.
	 */
	@Override
	public C wideningAux(C other) throws SemanticException {
		if (lessOrEqualEgliMilner(other)) {
			return extrapolationHeuristic(other).removeRedundancy().removeOverlapping();
		} else {
			return extrapolationHeuristic(EgliMilnerConnector(other)).removeRedundancy().removeOverlapping();
		}
	}

	/**
	 * Yields {@code true} if and only if this element is in Egli-Milner
	 * relation with the given one (represented as &le;<sub>EM</sub>). For two
	 * subset S<sub>1</sub> and S<sub>2</sub> of the domain of a lattice
	 * S<sub>1</sub> &le;<sub>EM</sub> S<sub>2</sub> iff: ( S<sub>1</sub>
	 * &le;<sub>S</sub> S<sub>2</sub> ) AND ( &forall; s<sub>2</sub> &ni;
	 * S<sub>2</sub>, &exist; s<sub>1</sub> &ni; S<sub>1</sub> : s<sub>1</sub>
	 * &le; s<sub>2</sub> ). Where
	 * {@link #lessOrEqualAux(NonRedundantPowersetOfBaseNonRelationalValueDomain)
	 * &le;<sub>S</sub>} is the less or equal relation between sets. This
	 * operation is not commutative.
	 * 
	 * @param other the other concrete element
	 * 
	 * @return {@code true} if and only if that condition holds
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public boolean lessOrEqualEgliMilner(C other) throws SemanticException {
		if (lessOrEqual(other)) {
			if (!isBottom()) {
				for (E s2 : other.elementsSet) {
					boolean existsLowerElement = false;
					for (E s1 : elementsSet) {
						if (s1.lessOrEqual(s2)) {
							existsLowerElement = true;
							break;
						}
					}
					if (!existsLowerElement)
						return false;
				}
			}
		} else
			return false;
		return true;
	}

	/**
	 * For two subset S<sub>1</sub> and S<sub>2</sub> of the domain of a lattice
	 * S<sub>1</sub> &le;<sub>S</sub> S<sub>2</sub> iff: &forall; s<sub>1</sub>
	 * &ni; S<sub>1</sub>, &exist; s<sub>2</sub> &ni; S<sub>2</sub> :
	 * s<sub>1</sub> &le; s<sub>2</sub>.
	 */
	@Override
	public boolean lessOrEqualAux(C other) throws SemanticException {
		for (E s1 : elementsSet) {
			boolean existsGreaterElement = false;
			for (E s2 : other.elementsSet) {
				if (s1.lessOrEqual(s2)) {
					existsGreaterElement = true;
					break;
				}
			}
			if (!existsGreaterElement)
				return false;
		}
		return true;
	}

	@Override
	public C evalNonNullConstant(Constant constant, ProgramPoint pp) throws SemanticException {
		C newSet = bottom();
		newSet.add(valueDomain.evalNonNullConstant(constant, pp));
		return newSet.removeRedundancy().removeOverlapping();
	}

	@Override
	public C evalUnaryExpression(UnaryOperator operator, C arg, ProgramPoint pp) throws SemanticException {
		Set<E> newSet = new HashSet<E>();
		for (E s : arg.elementsSet) {
			newSet.add(valueDomain.evalUnaryExpression(operator, s, pp));
		}
		return mk(newSet).removeRedundancy().removeOverlapping();
	}

	@Override
	public C evalBinaryExpression(BinaryOperator operator,
			C left,
			C right,
			ProgramPoint pp) throws SemanticException {
		Set<E> newSet = new HashSet<E>();
		for (E sLeft : left.elementsSet) {
			for (E sRight : right.elementsSet) {
				newSet.add(valueDomain.evalBinaryExpression(operator, sLeft, sRight, pp));
			}
		}
		return mk(newSet).removeRedundancy().removeOverlapping();
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(BinaryOperator operator,
			C left,
			C right,
			ProgramPoint pp) throws SemanticException {

		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		Set<Satisfiability> setSatisf = new HashSet<Satisfiability>();
		for (E sLeft : left.elementsSet) {
			for (E sRight : right.elementsSet) {
				setSatisf.add(valueDomain.satisfiesBinaryExpression(operator, sLeft, sRight, pp));
			}
		}
		if ((setSatisf.contains(Satisfiability.SATISFIED) && setSatisf.contains(Satisfiability.NOT_SATISFIED)) ||
				setSatisf.contains(Satisfiability.UNKNOWN))
			return Satisfiability.UNKNOWN;
		else if (setSatisf.contains(Satisfiability.SATISFIED))
			return Satisfiability.SATISFIED;
		else if (setSatisf.contains(Satisfiability.NOT_SATISFIED))
			return Satisfiability.NOT_SATISFIED;

		return Satisfiability.UNKNOWN;
	}

	@Override
	public boolean isBottom() {
		return elementsSet.isEmpty();
	}

	@Override
	public boolean isTop() {
		for (E element : elementsSet)
			if (element.isTop())
				return true;
		return false;
	}
}
