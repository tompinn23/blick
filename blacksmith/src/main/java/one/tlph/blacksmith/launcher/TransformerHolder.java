package one.tlph.blacksmith.launcher;

import one.tlph.blacksmith.api.launcher.ITransformationService;
import one.tlph.blacksmith.api.launcher.ITransformer;
import one.tlph.blacksmith.api.launcher.ITransformerVotingContext;
import one.tlph.blacksmith.api.launcher.TransformerVoteResult;

import javax.annotation.Nonnull;
import java.util.Set;

public class TransformerHolder<T> implements ITransformer<T> {
    private final ITransformer<T> wrapped;
    private final ITransformationService owner;

    public TransformerHolder(final ITransformer<T> wrapped, ITransformationService owner) {
        this.wrapped = wrapped;
        this.owner = owner;
    }

    @Nonnull
    @Override
    public T transform(final T input, final ITransformerVotingContext context) {
        return wrapped.transform(input, context);
    }

    @Nonnull
    @Override
    public TransformerVoteResult castVote(final ITransformerVotingContext context) {
        return wrapped.castVote(context);
    }

    @Nonnull
    @Override
    public Set<Target> targets() {
        return wrapped.targets();
    }

    @Override
    public String[] labels() {
        return wrapped.labels();
    }

    public ITransformationService owner() {
        return owner;
    }
}
