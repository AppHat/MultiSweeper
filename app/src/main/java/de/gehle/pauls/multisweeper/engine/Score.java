package de.gehle.pauls.multisweeper.engine;

import android.util.Log;

/**
 * @author Dima
 */
class Score {

    private int nrOfPlayers;
    private Integer[] score;

    /**
     * @param nrOfPlayers number of players to save score for
     */
    Score(int nrOfPlayers) {
        this.nrOfPlayers = nrOfPlayers;
        score = new Integer[nrOfPlayers];
        reset();
    }

//    public void inc(int playerId){
//        inc(playerId, 1);
//    }

    public void inc(int playerId, int score) {
        if (!inBounds(playerId)) {
            return;
        }
        this.score[playerId] += score;
    }

    public int getFinalScore(int playerId, int fieldSize, int mines, int time) {
        final int uncoveredFieldsFactor = 1;
        final int minesFactor = 2;
        final int timeFactor = 1000;

        if (!inBounds(playerId)) {
            // TODO throw an exception
            return 0;
        }

        int minedensity = (mines / fieldSize) * 100;
        if (minedensity < 10 || minedensity > 90) {
            minedensity = 1;
        }
        int seconds = time > 0 ? time : 1;

        Log.d("Score", "Uncovered: " + score[playerId]);
        Log.d("Score", "Minedensity: " + minedensity);
        Log.d("Score", "Timer: " + seconds);

        //return (score[playerId] * minedensity * 100) / seconds;
        if (score[playerId] > 0) {
            return uncoveredFieldsFactor * score[playerId] +
                    minesFactor * minedensity +
                    (timeFactor / seconds);
        } else {
            return 0;
        }
    }

    public int getPlace(int playerId) {
        if (!inBounds(playerId)) {
            return 0;
        }

        int place = 1;
        for (int i = 0; i < nrOfPlayers; ++i) {
            if (score[i] > score[playerId]) {
                ++place;
            }
        }
        return place;
    }

    public void reset() {
        for (int i = 0; i < nrOfPlayers; ++i) {
            reset(i);
        }
    }

    public void reset(int playerId) {
        if (!inBounds(playerId)) {
            return;
        }
        score[playerId] = 0;
    }

    private boolean inBounds(int i) {
        return i >= 0 && i < nrOfPlayers;
    }
}

