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

        public GameState(int turn, QuartoBoard currentBoard) {
            this.turn = turn;
            this.currentBoard = new QuartoBoard(currentBoard);
            this.sum = 0;
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
        MIN: random piece
        loop
        (pretty much same algorithm)
        MAX: random position
        MAX: random piece
        MIN: random position
        MIN: random piece
        ...
	 */
    @Override
    protected String pieceSelectionAlgorithm() {
        
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
                                System.out.println("Wining piece -: " + i);
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

        // Define tree of game states (Works as expected)
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

        // Test for game states tree
        // for (int i = 0; i < position; i++) {
        //     System.out.println(i + " - piece: " + gameStates[i].getPiece() + ", move: " + gameStates[i].getMove());
        // }
        // System.out.println(position + "\n\n");

        double highestExpectedScore = 100;
        int highestExpectedScorePosition = 0;
        final double MAX_SIMULATIONS = 100;
        double sum = 0;
        int simulations_multiplier = 0;
        boolean cont = true;
        int positionChecked = 0;
        double averageSum = 0;
        double totalSum = 0;
        int turn = 0;

        while(cont) {
            simulations_multiplier++;
            cont = false;
            positionChecked = 0;
            totalSum = 0;

            // Perform simulations for every game state in tree
            for (int i = 0; i < position; i++) {
                if(gameStates[i].getSum() <= averageSum) {
                    positionChecked++;
                    sum = 0;
                    int[] randomMove = new int[2];
                    int randomPieceID;
                    for (int j = 0; j < MAX_SIMULATIONS; j++) {
                        QuartoBoard currentBoard = new QuartoBoard(gameStates[i].getCurrentBoard());
                        turn = 0;
                        double currentScore = 0;
                        // One game/simulation
                        while (true) {
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
                        }
                        sum += currentScore;
                    }
                    double newSum = gameStates[i].getSum() + sum;
                    gameStates[i].setSum(newSum);
                    totalSum = totalSum + newSum;
                }
            }

            averageSum = (double) (totalSum / positionChecked);

            if(this.getMillisecondsFromTimer() <= 6000) {
                cont = true;
            }
        }

        System.out.println("Number of simulations: " + MAX_SIMULATIONS*simulations_multiplier);

        // int positive = 0, neagtive = 0, zero = 0;
        // for(int i = 0; i < position; i++) {
        //     // System.out.println("Piece: " + gameStates[i].getPiece() + ", Position: " + gameStates[i].getMove() + ", Expected Value: " + gameStates[i].getSum() / MAX_SIMULATIONS);
        //     if(gameStates[i].getSum() == 0) {
        //         zero++;
        //     } else if(gameStates[i].getSum() > 0) {
        //         positive++;
        //     } else { 
        //         neagtive++;
        //     }
        // }

        // System.out.println("Majority of games Won -: " + positive + " Majority of games Lost -: " + neagtive + " Majority of games Draw -: " + zero);

        double availablePieces[] = new double[32];

        for (int i = 0; i < 32; i++){
            availablePieces[i] = 11;
        }

        int count = 0;
        int currentPiece = 0;

        // for (int i = 0; i < position; i++) {
        //     double result = (double)(gameStates[i].getSum() / MAX_SIMULATIONS);
        //     gameStates[i].setExpectedScore(result);
        // }

        for (int i = 0; i < position; i++) {
            if(gameStates[i].getSum() <= averageSum) {
                double result = (double)(gameStates[i].getSum() / (MAX_SIMULATIONS * simulations_multiplier));
                gameStates[i].setExpectedScore(result);
            }
            // System.out.println(i + " Expected Value -: " + gameStates[i].getExpectedScore() + " Sum -: " + gameStates[i].getSum());
        }

        // MIN selects the move
        for (int i = 0; i < position; i++) {
            if(gameStates[i].getSum() <= averageSum) {
                if(availablePieces[gameStates[i].getPiece()] > gameStates[i].getExpectedScore()) {
                    availablePieces[gameStates[i].getPiece()] = gameStates[i].getExpectedScore();
                }
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
            }
            else if (availablePieces[i] > largestSmallestValue && availablePieces[i] < 11) {
                largestSmallestValue = availablePieces[i];
                largestSmallestValueLocation = i;
            }
        }

        // Arrays.sort(availablePieces);
        // largestSmallestValueLocation = 0;

        String BinaryString = String.format("%5s", Integer.toBinaryString(largestSmallestValueLocation)).replace(' ', '0');
        
        System.out.println("Piece Selected decimal: " + largestSmallestValueLocation);
        System.out.println("Piece Selected binary: " + BinaryString);
        
        System.out.println("Time Taken -: " + this.getMillisecondsFromTimer());

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

        // If no winning move is found in the above code, then return a random (unoccupied) square
        // int[] move = new int[2];
        // QuartoBoard copyBoard = new QuartoBoard(this.quartoBoard);
        // move = copyBoard.chooseRandomPositionNotPlayed(100);

        // return move[0] + "," + move[1];

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
        double highestExpectedScore = -100;
        int highestExpectedScorePosition = 0;
        final double MAX_SIMULATIONS = 100;
        double sum = 0;
        int simulations_multiplier = 0;
        boolean cont = true;
        int positionChecked = 0;
        double totalSum = 0;
        double averageSum = 0;
        int turn = 0;
        while(cont) {
            simulations_multiplier++;
            cont = false;
            positionChecked = 0;
            totalSum = 0;
            for (int i = 0; i < position; i++) {
                if(gameStates[i].getSum() >= averageSum) {
                    positionChecked++;
                    turn = 0;
                    // Perform random runs
                    QuartoBoard currentGameStateBoard = new  QuartoBoard(gameStates[i].getCurrentBoard());
                    sum = 0;
                    int[] randomMove = new int[2];
                    int randomPieceID = gameStates[i].getPiece();
                    for (int j = 0; j < MAX_SIMULATIONS; j++) {
                        turn = 0;
                        double currentScore = 0;
                        QuartoBoard currentBoard = new QuartoBoard(currentGameStateBoard);
                        while (true) {
                            // Choose random move
                            randomMove = currentBoard.chooseRandomPositionNotPlayed(50);
                            currentBoard.insertPieceOnBoard(randomMove[0], randomMove[1], randomPieceID);
                            
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

                            // Choose random piece
                            randomPieceID = currentBoard.chooseRandomPieceNotPlayed(50);

                            // Switch turns
                            if(turn == 1) {
                                turn = 0;
                            } else {
                                turn = 1;
                            }
                        }
                        sum += currentScore;
                    }
                    double newSum = gameStates[i].getSum() + sum;
                    gameStates[i].setSum(newSum);
                    totalSum = totalSum + newSum;
                }
            }
            averageSum = (double) (totalSum / positionChecked);
            // System.out.println("Average sum: " + averageSum);

            if (this.getMillisecondsFromTimer() <= 6000) {
                cont = true;
            }
        }

        System.out.println("Number of simulations: " + MAX_SIMULATIONS*simulations_multiplier);

        int numOverAverage = 0;
        for (int i = 0; i < position; i++) {
            if(gameStates[i].getSum() >= averageSum) {
                numOverAverage++;
                double result = (double)(gameStates[i].getSum() / (MAX_SIMULATIONS*simulations_multiplier));
                gameStates[i].setExpectedScore(result);
                // System.out.println(result);
                if (result > highestExpectedScore) {
                    highestExpectedScore = result;
                    highestExpectedScorePosition = i;
                }
            }
        }
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
