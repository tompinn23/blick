package one.tlph.blacksmith.launcher;

import java.util.List;

public class VoteDeadlockException extends RuntimeException {
    <T> VoteDeadlockException(List<TransformerVote<T>> votes, Class<?> aClass) {
    }
}
