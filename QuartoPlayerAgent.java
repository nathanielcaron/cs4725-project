/**
  * Quarto Player Agent
  * 
  * CS 4725 - Introduction to Artificial Intelligence
  * Dr. Michael Fleming
  * 
  * Nathaniel Caron, 3598979
  * Sahil Saini, 3562310
  */

import java.util.Arrays;

public class QuartoPlayerAgent extends QuartoAgent {

    // Class to represent a game state node
    public class GameState {
        private double expectedScore;
        private double sum;
        // MAX = 1, MIN = 0
        private int turn;
        private int piece = -1;
        private int move[] = {-1, -1};
        private QuartoBoard currentBoard;
        private boolean aboveAvearge;

        public GameState(int turn, QuartoBoard currentBoard) {
            this.turn = turn;
            this.currentBoard = new QuartoBoard(currentBoard);
            this.sum = 0;
            this.aboveAvearge = false;
        }

        public void setPiece(int pieceId) {
            this.piece = pieceId;
        }

        public int getPiece() {
            return this.piece;
        }

        public void setTurn(int Newturn) {
            this.turn = Newturn;
        }

        public int getTurn() {
            return this.turn;
        }

        public void setMove(int row, int column) {
            this.move[0] = row;
            this.move[1] = column;
        }
        
        public String getMove() {
            return this.move[0] + "," + this.move[1];
        }

        public void setExpectedScore(double expectedScore) {
            this.expectedScore = expectedScore;
        }

        public double getExpectedScore() {
            return this.expectedScore;
        }

        public void setSum(double sum) {
            this.sum = sum;
        }

        public double getSum() {
            return this.sum;
        }

        public QuartoBoard getCurrentBoard() {
            return this.currentBoard;
        }

        public void setAboveAverage(boolean aboveAvearge) {
            this.aboveAvearge = aboveAvearge;
        }

        public boolean getAboveAvearge() {
            return this.aboveAvearge;
        }

        public void printGameState() {
            System.out.println("move: " + this.move[0] + ", " + this.move[1] + " Piece: " + this.piece);
        }
    }

    public QuartoPlayerAgent(GameClient gameClient, String stateFileName) {
        // because super calls one of the super class constructors(you can overload constructors), you need to pass the parameters required.
        super(gameClient, stateFileName);
    }

    public static void main(String[] args) {
        //start the server
        GameClient gameClient = new GameClient();

        String ip = null;
        String stateFileName = null;
        //IP must be specified
        if(args.length > 0) {
            ip = args[0];
        } else {
            System.out.println("No IP Specified");
            System.exit(0);
        }
        if (args.length > 1) {
            stateFileName = args[1];
        }

        gameClient.connectToServer(ip, 4321);
        QuartoPlayerAgent quartoAgent = new QuartoPlayerAgent(gameClient, stateFileName);
        quartoAgent.play();

        gameClient.closeConnection();
    }

    /*
	 * Function to find the most optimal piece
     *  Monte Carlo:
        Start state is current board
        (eliminate potentially winning pieces)
        MAX: branches for all pieces available (choose piece)
        MIN: branches for each position available (choose position)
        RANDOM MOVES START HERE
        loop
        MIN: random piece
        MAX: random position
        MAX: random piece
        MIN: random position
        MIN: random piece
        ...
	 */
    @Override
    protected String pieceSelectionAlgorithm() {
        //some useful lines:
        //String BinaryString = String.format("%5s", Integer.toBinaryString(pieceID)).replace(' ', '0');
        
        this.startTimer();

        GameState gameStates[] = new GameState[800];
        int position = 0;

        int badPieces[] = new int[32];
        Arrays.fill(badPieces,0);

        boolean skip = false;
        for (int i = 0; i < this.quartoBoard.getNumberOfPieces(); i++) {
            skip = false;
            if (!this.quartoBoard.isPieceOnBoard(i)) {
                for (int row = 0; row < this.quartoBoard.getNumberOfRows(); row++) {
                    for (int col = 0; col < this.quartoBoard.getNumberOfColumns(); col++) {
                        if (!this.quartoBoard.isSpaceTaken(row, col)) {
                            QuartoBoard copyBoard = new QuartoBoard(this.quartoBoard);
                            copyBoard.insertPieceOnBoard(row, col, i);
                            if (copyBoard.checkRow(row) || copyBoard.checkColumn(col) || copyBoard.checkDiagonals()) {
                                System.out.println("Bad piece -: " + i);
                                skip = true;
                                break;
                            }
                        }
                    }
                    if (skip) {
                        badPieces[i] = 1;
                        break;
                    }
                }
            }
        }

        // Define tree of game states
        for (int i = 0; i < this.quartoBoard.getNumberOfPieces(); i++) {
            // MAX chooses a piece
            if (!this.quartoBoard.isPieceOnBoard(i) && badPieces[i] != 1) {
                for (int row = 0; row < this.quartoBoard.getNumberOfRows(); row++) {
                    for (int col = 0; col < this.quartoBoard.getNumberOfColumns(); col++) {
                        if (!this.quartoBoard.isSpaceTaken(row, col)) {
                            // MIN chooses a move
                            QuartoBoard copyBoard = new QuartoBoard(this.quartoBoard);
                            copyBoard.insertPieceOnBoard(row, col, i);
                            gameStates[position] = new GameState(0, copyBoard);
                            gameStates[position].setMove(row, col);
                            gameStates[position].setPiece(i);
                            position++;
                        }
                    }
                }
            }
        }

        // If no game states were created (tree is empty), then all pieces left could lead to the opponent winning
        if (position == 0) {
            System.out.println("All pieces left can allow the other player to win");
            // Define tree of game states (Works as expected)
            for (int i = 0; i < this.quartoBoard.getNumberOfPieces(); i++) {
                // MAX chooses a piece
                if (!this.quartoBoard.isPieceOnBoard(i)) {
                    for (int row = 0; row < this.quartoBoard.getNumberOfRows(); row++) {
                        for (int col = 0; col < this.quartoBoard.getNumberOfColumns(); col++) {
                            if (!this.quartoBoard.isSpaceTaken(row, col)) {
                                // MIN chooses a move
                                QuartoBoard copyBoard = new QuartoBoard(this.quartoBoard);
                                copyBoard.insertPieceOnBoard(row, col, i);
                                gameStates[position] = new GameState(0, copyBoard);
                                gameStates[position].setMove(row, col);
                                gameStates[position].setPiece(i);
                                position++;
                            }
                        }
                    }
                }
            }
        }

        // TODO: Remove this
        // Test for game states tree
        // for (int i = 0; i < position; i++) {
        //     System.out.println(i + " - piece: " + gameStates[i].getPiece() + ", move: " + gameStates[i].getMove());
        // }
        // System.out.println(position + "\n\n");

        double highestExpectedScore = 100;
        int highestExpectedScorePosition = 0;
        final double SIMULATIONS = 100;
        int simulationMultiplier = 0;
        double sum = 0;
        int turn = 0;

        boolean cont = true;
        long simulationsStartTime;
        long simulationsStopTime;
        long duration;
        long currentTime;

        while (cont) {
            cont = false;
            simulationsStartTime = System.nanoTime();
            simulationMultiplier++;

            // Perform simulations for every game state in tree
            for (int i = 0; i < position; i++) {
                sum = 0;
                int[] randomMove = new int[2];
                int randomPieceID;
                for (int j = 0; j < SIMULATIONS; j++) {
                    QuartoBoard currentBoard = new QuartoBoard(gameStates[i].getCurrentBoard());
                    turn = 0;
                    double currentScore = 0;
                    // One game/simulation
                    while (true) {
                        // Check if game is over
                        if (checkIfGameIsWon(currentBoard)) {
                            if (turn == 1) {
                                currentScore = 10;
                                break;
                            } else {
                                currentScore = -10;
                                break;
                            }
                        } else if (checkIfGameIsDraw(currentBoard)) {
                            currentScore = 0;
                            break;
                        }

                        // Choose a random piece
                        randomPieceID = currentBoard.chooseRandomPieceNotPlayed(100);
                        
                        // Switch turns
                        if(turn == 1) {
                            turn = 0;
                        } else {
                            turn = 1;
                        }

                        // Choose random move
                        randomMove = currentBoard.chooseRandomPositionNotPlayed(100);
                        currentBoard.insertPieceOnBoard(randomMove[0], randomMove[1], randomPieceID);
                    }
                    sum += currentScore;
                }
                double newSum = gameStates[i].getSum() + sum;
                gameStates[i].setSum(newSum);
            }

            // Check if we still have time for more simulations
            simulationsStopTime = System.nanoTime();
            duration = (simulationsStopTime - simulationsStartTime)/1000000;
            currentTime = this.getMillisecondsFromTimer();
            System.out.println("*** *** *** *** ***");
            System.out.println("100 Simulations took: " + duration + " milliseconds.");
            if ((9000 - currentTime) > duration) {
                System.out.println("Current time: " + currentTime + " still have time " + (int)(9000 - currentTime) + " milliseconds");
                cont = true;
            }
            System.out.println("*** *** *** *** ***");
        }

        System.out.println("### ### Completed " + SIMULATIONS*simulationMultiplier + " simulations ### ###");

        // System.out.println("Majority of games Won -: " + positive + " Majority of games Lost -: " + neagtive + " Majority of games Draw -: " + zero);

        double availablePieces[] = new double[32];
        for (int i = 0; i < 32; i++){
            availablePieces[i] = 11;
        }

        int currentPiece = 0;

        for (int i = 0; i < position; i++) {
            double result = (double)(gameStates[i].getSum() / (SIMULATIONS*simulationMultiplier));
            gameStates[i].setExpectedScore(result);
        }

        // MIN selects the move
        for (int i = 0; i < position; i++) {
            if(availablePieces[gameStates[i].getPiece()] > gameStates[i].getExpectedScore()) {
                availablePieces[gameStates[i].getPiece()] = gameStates[i].getExpectedScore();
            }
        }

        // MAX selects the piece
        double largestSmallestValue = availablePieces[0];
        int largestSmallestValueLocation = 0;
        // System.out.println("availablePieces[" + 0 + "] = " + availablePieces[0]);
        for (int i = 1; i < 32; i++){
            // System.out.println("availablePieces[" + i + "] = " + availablePieces[i]);
            if(largestSmallestValue > 10) {
                largestSmallestValue = availablePieces[i];
                largestSmallestValueLocation = i;
            } else if (availablePieces[i] > largestSmallestValue && availablePieces[i] < 11) {
                largestSmallestValue = availablePieces[i];
                largestSmallestValueLocation = i;
            }
        }

        String BinaryString = String.format("%5s", Integer.toBinaryString(largestSmallestValueLocation)).replace(' ', '0');
        
        System.out.println("Piece Selected decimal: " + largestSmallestValueLocation);
        System.out.println("Piece Selected binary: " + BinaryString);

        currentTime = this.getMillisecondsFromTimer();
        System.out.println("Time taken: " + currentTime + " milliseconds");

        return BinaryString;
    }

    /*
     * Function to find the most optimal position
     *  Monte Carlo:
        Start state is current board
        MAX: branches for each moves available (Choose position)
        MAX: branches for all pieces available (choose piece)
        RANDOM MOVES START HERE
        MIN: random move
        MIN: random piece
        MAX: random move
        MAX: random piece
        ...
     */
    @Override
    protected String moveSelectionAlgorithm(int pieceID) {
        this.startTimer();

        // First try to find a winning move
        for (int row = 0; row < this.quartoBoard.getNumberOfRows(); row++) {
            for (int col = 0; col < this.quartoBoard.getNumberOfColumns(); col++) {
                if (!this.quartoBoard.isSpaceTaken(row, col)) {
                    QuartoBoard copyBoard = new QuartoBoard(this.quartoBoard);
                    copyBoard.insertPieceOnBoard(row, col, pieceID);
                    if (copyBoard.checkRow(row) || copyBoard.checkColumn(col) || copyBoard.checkDiagonals()) {
                        return row + "," + col;
                    }
                }
            }
        }

        // Define tree of game states
        GameState gameStates[] = new GameState[800];
        int position = 0;
        for (int row = 0; row < this.quartoBoard.getNumberOfRows(); row++) {
            for (int col = 0; col < this.quartoBoard.getNumberOfColumns(); col++) {
                if (!this.quartoBoard.isSpaceTaken(row, col)) {
                    // MAX chooses a move
                    QuartoBoard copyBoard = new QuartoBoard(this.quartoBoard);
                    copyBoard.insertPieceOnBoard(row, col, pieceID);
                    // MAX chooses a piece
                    for (int pieceNum = 0; pieceNum < copyBoard.getNumberOfPieces(); pieceNum++) {
                        if (!copyBoard.isPieceOnBoard(pieceNum)) {
                            gameStates[position] = new GameState(1, copyBoard);
                            gameStates[position].setMove(row, col);
                            gameStates[position].setPiece(pieceNum);
                            // Set turn to MIN
                            // gameStates[position].setTurn(0);
                            position++;
                        }
                    }
                }
            }
        }

        // Calculate avearge value, and run simulations on the states with above average expected value

        // Perform random runs for every game state in tree
        final double MAX_SIMULATIONS = 100;
        double sum = 0;
        int simulationMultiplierRound1 = 0;
        int simulationMultiplierRound2 = 0;
        boolean cont = true;
        double totalSum = 0;
        double averageSum = 0;

        // First round
        // < 6 seconds
        cont = true;
        long simulationsStartTime;
        long simulationsStopTime;
        long duration;
        long currentTime;
        int stateCounter = 0;

        while (cont) {
            cont = false;
            simulationsStartTime = System.nanoTime();
            simulationMultiplierRound1++;

            for (int i = 0; i < position; i++) {
                int turn = 0;
                // Perform random runs
                QuartoBoard currentGameStateBoard = new  QuartoBoard(gameStates[i].getCurrentBoard());
                sum = 0;
                int[] randomMove = new int[2];
                int randomPieceID = gameStates[i].getPiece();
                for (int j = 0; j < MAX_SIMULATIONS; j++) {
                    turn = 1;
                    double currentScore = 0;
                    QuartoBoard currentBoard = new QuartoBoard(currentGameStateBoard);
                    while (true) {
                        // Check if game is over
                        if (checkIfGameIsWon(currentBoard)) {
                            if (turn == 1) {
                                currentScore = 10;
                                break;
                            } else {
                                currentScore = -10;
                                break;
                            }
                        } else if (checkIfGameIsDraw(currentBoard)) {
                            currentScore = 0;
                            break;
                        }

                        // Switch turns
                        if(turn == 1) {
                            turn = 0;
                        } else {
                            turn = 1;
                        }

                        // Choose random move
                        randomMove = currentBoard.chooseRandomPositionNotPlayed(50);
                        currentBoard.insertPieceOnBoard(randomMove[0], randomMove[1], randomPieceID);

                        // Choose random piece
                        randomPieceID = currentBoard.chooseRandomPieceNotPlayed(50);
                    }
                    sum += currentScore;
                }
                double newSum = gameStates[i].getSum() + sum;
                gameStates[i].setSum(newSum);
                totalSum = totalSum + sum;
                if(simulationMultiplierRound1 == 1) {
                    stateCounter++;
                }
            }

            // Check if we still have time for more simulations
            simulationsStopTime = System.nanoTime();
            duration = (simulationsStopTime - simulationsStartTime)/1000000;
            currentTime = this.getMillisecondsFromTimer();
            System.out.println("*** *** *** *** ***");
            System.out.println("Round 1");
            System.out.println("100 Simulations took: " + duration + " milliseconds.");
            if ((6000 - currentTime) > duration) {
                System.out.println("Current time: " + currentTime + " still have " + (int)(6000 - currentTime) + " milliseconds");
                cont = true;
            }
            System.out.println("*** *** *** *** ***");
        }

        averageSum = (double) totalSum / stateCounter;
        System.out.println("Round 1, Average Sum: " + averageSum + " State counter: " + stateCounter);


        // Seconds round
        // < 9 seconds
        cont = true;
        totalSum = 0;
        stateCounter = 0;

        for (int i = 0; i < position; i++) {
            if(gameStates[i].getSum() >= averageSum) {
                gameStates[i].setAboveAverage(true);
            }
        }

        while (cont) {
            cont = false;
            simulationsStartTime = System.nanoTime();
            simulationMultiplierRound2++;

            for (int i = 0; i < position; i++) {
                if(gameStates[i].getAboveAvearge()) {
                    int turn = 0;
                    // Perform random runs
                    QuartoBoard currentGameStateBoard = new  QuartoBoard(gameStates[i].getCurrentBoard());
                    sum = 0;
                    int[] randomMove = new int[2];
                    int randomPieceID = gameStates[i].getPiece();
                    for (int j = 0; j < MAX_SIMULATIONS; j++) {
                        turn = 1;
                        double currentScore = 0;
                        QuartoBoard currentBoard = new QuartoBoard(currentGameStateBoard);
                        while (true) {
                            // Check if game is over
                            if (checkIfGameIsWon(currentBoard)) {
                                if (turn == 1) {
                                    currentScore = 10;
                                    break;
                                } else {
                                    currentScore = -10;
                                    break;
                                }
                            } else if (checkIfGameIsDraw(currentBoard)) {
                                currentScore = 0;
                                break;
                            }

                            // Switch turns
                            if(turn == 1) {
                                turn = 0;
                            } else {
                                turn = 1;
                            }

                            // Choose random move
                            randomMove = currentBoard.chooseRandomPositionNotPlayed(100);
                            currentBoard.insertPieceOnBoard(randomMove[0], randomMove[1], randomPieceID);

                            // Choose random piece
                            randomPieceID = currentBoard.chooseRandomPieceNotPlayed(100);
                        }
                        sum += currentScore;
                    }
                    double newSum = gameStates[i].getSum() + sum;
                    gameStates[i].setSum(newSum);
                    totalSum = totalSum + sum;
                    if(simulationMultiplierRound2 == 1) {
                        stateCounter++;
                    }
                }
            }

            // Check if we still have time for more simulations
            simulationsStopTime = System.nanoTime();
            duration = (simulationsStopTime - simulationsStartTime)/1000000;
            currentTime = this.getMillisecondsFromTimer();
            System.out.println("*** *** *** *** ***");
            System.out.println("Round 2");
            System.out.println("100 Simulations took: " + duration + " milliseconds.");
            if ((9000 - currentTime) > duration) {
                System.out.println("Current time: " + currentTime + " still have " + (int)(9000 - currentTime) + " milliseconds");
                cont = true;
            }
            System.out.println("*** *** *** *** ***");
        }

        averageSum = (double) totalSum / stateCounter;
        System.out.println("Round 2, Average Sum -: " + averageSum + " State counter: " + stateCounter);

        int numOverAverage = 0;
        double highestExpectedScore = -100;
        int highestExpectedScorePosition = -1;
        double totalExpectedValue = 0;
        for (int i = 0; i < position; i++) {
            if(gameStates[i].getAboveAvearge()) {
                numOverAverage++;
                double result = (double)(gameStates[i].getSum() / (MAX_SIMULATIONS*simulationMultiplierRound2 + MAX_SIMULATIONS*simulationMultiplierRound1));
                totalExpectedValue += result;
                gameStates[i].setExpectedScore(result);
                if (result > highestExpectedScore) {
                    highestExpectedScore = result;
                    highestExpectedScorePosition = i;
                }
            }
        }

        System.out.println("Number of simulations for round 1: " + MAX_SIMULATIONS*simulationMultiplierRound1);
        System.out.println("Number of simulations for round 2: " + MAX_SIMULATIONS*simulationMultiplierRound2);
        System.out.println("Total number of simulations " + (MAX_SIMULATIONS*simulationMultiplierRound2 + MAX_SIMULATIONS*simulationMultiplierRound1));
        System.out.println("Average Expected Value: " + (double) (totalExpectedValue / numOverAverage));
        System.out.println("Expected Value of the piece selcted: " + gameStates[highestExpectedScorePosition].getExpectedScore());
        
        System.out.println("Number of game states over average sum: " + numOverAverage);

        System.out.println("Time Taken -: " + this.getMillisecondsFromTimer());

        return gameStates[highestExpectedScorePosition].getMove();
    }

    //loop through board and see if the game is in a won state
    private boolean checkIfGameIsWon(QuartoBoard board) {

        //loop through rows
        for(int i = 0; i < NUMBER_OF_ROWS; i++) {
            //gameIsWon = this.quartoBoard.checkRow(i);
            if (board.checkRow(i)) {
                // System.out.println("Win via row: " + (i) + " (zero-indexed)");
                return true;
            }

        }
        //loop through columns
        for(int i = 0; i < NUMBER_OF_COLUMNS; i++) {
            //gameIsWon = this.quartoBoard.checkColumn(i);
            if (board.checkColumn(i)) {
                // System.out.println("Win via column: " + (i) + " (zero-indexed)");
                return true;
            }

        }

        //check Diagonals
        if (board.checkDiagonals()) {
            // System.out.println("Win via diagonal");
            return true;
        }

        return false;
    }

    //loop through board and see if the game is in a won state
	private boolean checkIfGameIsDraw(QuartoBoard board) {
		return board.checkIfBoardIsFull();
	}
}
