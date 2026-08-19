package dev.hxrry.tinker.property;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class StateProperty<T extends Comparable<T>> implements TinkerProperty {

    private final Category category;
    private final String id;
    private final String stateName;
    private final Class<T> valueType;
    private final Predicate<BlockState> gate;

    private StateProperty(Category category, String id, String stateName, Class<T> valueType,
            Predicate<BlockState> gate) {
        this.category = Objects.requireNonNull(category, "category");
        this.id = Objects.requireNonNull(id, "id");
        this.stateName = Objects.requireNonNull(stateName, "stateName");
        this.valueType = Objects.requireNonNull(valueType, "valueType");
        this.gate = Objects.requireNonNull(gate, "gate");
    }

    public static <T extends Comparable<T>> TinkerProperty of(Category category, String id,
            Class<T> valueType, Predicate<BlockState> gate) {
        return new StateProperty<>(category, id, id, valueType, gate);
    }

    public static <T extends Comparable<T>> TinkerProperty named(Category category, String id,
            String stateName, Class<T> valueType, Predicate<BlockState> gate) {
        return new StateProperty<>(category, id, stateName, valueType, gate);
    }

    public static TinkerProperty bool(Category category, String id, Predicate<BlockState> gate) {
        return of(category, id, Boolean.class, gate);
    }

    public static TinkerProperty ints(Category category, String id, Predicate<BlockState> gate) {
        return of(category, id, Integer.class, gate);
    }

    public static TinkerProperty ints(Category category, String id, String stateName,
            Predicate<BlockState> gate) {
        return named(category, id, stateName, Integer.class, gate);
    }

    public static <E extends Enum<E> & Comparable<E>> TinkerProperty enums(Category category, String id,
            Class<E> enumType, Predicate<BlockState> gate) {
        return of(category, id, enumType, gate);
    }

    @Override
    public Category category() {
        return category;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean appliesTo(BlockState state) {
        return property(state) != null && gate.test(state);
    }

    @Override
    public String render(BlockState state) {
        Property<T> property = property(state);
        if (property == null) {
            return "";
        }
        return name(state.getValue(property));
    }

    @Override
    public BlockState cycle(BlockState state, int direction) {
        Property<T> property = property(state);
        if (property == null) {
            return state;
        }
        List<T> candidates = ordered(property);
        if (candidates.isEmpty()) {
            return state;
        }
        int current = candidates.indexOf(state.getValue(property));
        int next = current < 0 ? 0 : Math.floorMod(current + direction, candidates.size());
        return state.setValue(property, candidates.get(next));
    }

    private List<T> ordered(Property<T> property) {
        List<T> values = new ArrayList<>(property.getPossibleValues());
        values.sort(Comparable::compareTo);
        return values;
    }

    private String name(T value) {
        return value instanceof Enum<?> constant ? constant.name() : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private Property<T> property(BlockState state) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(stateName);
        if (property == null || !valueType.equals(property.getValueClass())) {
            return null;
        }
        return (Property<T>) property;
    }

    @Override
    public String toString() {
        return key();
    }
}
