package stincmale.sandbox.examples.self;

final class CountingCopyOnAppend extends AbstractCopyOnAppend<CountingCopyOnAppend> {
    final int appends;

    CountingCopyOnAppend(final String value) {
        this(value, 0);
    }

    private CountingCopyOnAppend(final String value, final int appends) {
        super(value);
        this.appends = appends;
    }

    @Override
    CountingCopyOnAppend newSelf(final String value) {
        return new CountingCopyOnAppend(value, appends + 1);
    }
}
