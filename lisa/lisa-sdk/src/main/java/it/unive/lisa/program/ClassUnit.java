package it.unive.lisa.program;

import it.unive.lisa.program.cfg.AbstractCodeMember;
import it.unive.lisa.program.cfg.CodeLocation;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.collections4.CollectionUtils;

/**
 * A {@link CompilationUnit} representing a concrete class that can be
 * instantiated.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ClassUnit extends CompilationUnit {

	/**
	 * The collection of compilation units this unit directly inherits from.
	 * This collection may contain both concrete and abstract compilation unit.
	 */
	private final List<ClassUnit> superclasses;

	/**
	 * The collection of interface units this unit directly inherits from.
	 */
	private final List<InterfaceUnit> interfaces;

	/**
	 * Builds a compilation unit, defined at the given program point.
	 * 
	 * @param location the location where the unit is define within the source
	 *                     file
	 * @param name     the name of the unit
	 * @param sealed   whether or not this unit is sealed, meaning that it
	 *                     cannot be used as super unit of other compilation
	 *                     units
	 */
	public ClassUnit(CodeLocation location, String name, boolean sealed) {
		super(location, name, sealed);
		Objects.requireNonNull(location, "The location of a unit cannot be null.");
		superclasses = new LinkedList<>();
		interfaces = new LinkedList<>();
	}

	@Override
	public Collection<CompilationUnit> getImmediateAncestors() {
		return CollectionUtils.union(superclasses, interfaces);
	}

	/**
	 * Yields the list of {@link ClassUnit}s that this class inherits from.
	 * 
	 * @return the superclasses
	 */
	public List<ClassUnit> getSuperclasses() {
		return superclasses;
	}

	/**
	 * Yields the list of {@link InterfaceUnit}s implemented by this class.
	 * 
	 * @return the interfaces
	 */
	public List<InterfaceUnit> getInterfaces() {
		return interfaces;
	}

	/**
	 * Adds a new {@link ClassUnit} as superclass of this unit.
	 * 
	 * @param unit the unit to add
	 * 
	 * @return {@code true} if the collection of superclasses changed as a
	 *             result of the call
	 */
	public final boolean addSuperclass(ClassUnit unit) {
		return superclasses.add(unit);
	}

	/**
	 * Adds a new {@link InterfaceUnit} as interface of this unit.
	 * 
	 * @param unit the unit to add
	 * 
	 * @return {@code true} if the collection of interfaces changed as a result
	 *             of the call
	 */
	public final boolean addInterface(InterfaceUnit unit) {
		return interfaces.add(unit);
	}

	@Override
	public boolean isInstanceOf(CompilationUnit unit) {
		return this == unit || (hierarchyComputed ? unit.instances.contains(this)
				: getImmediateAncestors().stream().anyMatch(u -> u.isInstanceOf(unit)));
	}

	private final void addInstance(ClassUnit unit) throws ProgramValidationException {
		if (superclasses.contains(unit))
			throw new ProgramValidationException("Found loop in compilation units hierarchy: " + unit
					+ " is both a super unit and an instance of " + this);
		instances.add(unit);

		for (ClassUnit sup : superclasses)
			sup.addInstance(unit);

		for (InterfaceUnit sup : interfaces)
			sup.addInstance(unit);
	}

	@Override
	public void validateAndFinalize() throws ProgramValidationException {
		if (hierarchyComputed)
			return;

		addInstance(this);
		if (canBeInstantiated() && !searchCodeMembers(cm -> cm instanceof AbstractCodeMember, false).isEmpty())
			throw new ProgramValidationException(
					this + " is not an abstract class and it cannot have abstract cfgs.");

		super.validateAndFinalize();
	}

	@Override
	public boolean canBeInstantiated() {
		return true;
	}

	@Override
	public boolean addAncestor(CompilationUnit unit) {
		if (unit instanceof ClassUnit)
			return superclasses.add((ClassUnit) unit);
		else
			return interfaces.add((InterfaceUnit) unit);
	}
}
