package one.tlph.blacksmith.launcher;

import one.tlph.blacksmith.api.launcher.ITransformer;
import one.tlph.blacksmith.api.launcher.TransformerVoteResult;

public class TransformerVote<T> {
    private final ITransformer<T> transformer;
    private final TransformerVoteResult result;

    TransformerVote(TransformerVoteResult vr, ITransformer<T> transformer) {
        this.transformer = transformer;
        this.result = vr;
    }

    TransformerVoteResult getResult() {
        return result;
    }

    ITransformer<T> getTransformer() {
        return transformer;
    }
}
