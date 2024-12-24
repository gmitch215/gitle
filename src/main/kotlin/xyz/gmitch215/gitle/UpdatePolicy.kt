package xyz.gmitch215.gitle

/**
 * Represents an update policy for a repository.
 */
enum class UpdatePolicy(
    /**
     * The millisecond interval for the update policy. `-1` means the policy is not time-based.
     */
    val millis: Long = -1L
) {
    /**
     * Always update the repository when the task is run.
     */
    ALWAYS(0),

    /**
     * Only update the repository when the task is run and the repository is not already cloned.
     */
    IF_MISSING,

    /**
     * Only update the repository when the task is run and the repository is out of date.
     */
    IF_OUT_OF_DATE,

    /**
     * Update the repository every hour.
     */
    EVERY_HOUR(60 * 60 * 1000),

    /**
     * Update the repository every day.
     */
    EVERY_DAY(24 * 60 * 60 * 1000),

    /**
     * Update the repository every week.
     */
    EVERY_WEEK(7 * 24 * 60 * 60 * 1000),
}