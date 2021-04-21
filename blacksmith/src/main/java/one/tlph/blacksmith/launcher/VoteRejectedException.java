package one.tlph.blacksmith.launcher;

import java.util.List;

public class VoteRejectedException extends RuntimeException {
    <T> VoteRejectedException(List<TransformerVote<T>> votes, Class<?> aClass) {
    }
}
