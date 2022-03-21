/**
  * Quarto Player Agent
  * 
  * CS 4725 - Introduction to Artificial Intelligence
  * Dr. Michael Fleming
  *
  * Programming Project
  * 
  * Nathaniel Caron
  * Sahil Saini
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
    * Eliminate potentially winning pieces for the opponent, perform Monte Carlo simulations on remaining game states
    * @return  a String of the binary value of the piece selected
    */
    @Override
    protected String pieceSelectionAlgorithm() {
        // Start timer for whole method
        this.startTimer();

        // Initialize array of game states representing tree of GameState nodes
        GameState gameStates[] = new GameState[800];
        int position = 0;

        // Initialize array of potentially winning pieces for the opponent
        int badPieces[] = new int[32];
        Arrays.fill(badPieces, 0);

        // Determine the potentially winning pieces for the opponent (bad pieces)
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

        // Define tree of game states (fill GameStates array)
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

        // Game states array is empty
        // All pieces remaining could lead to the opponent winning (include all pieces remaining)
        if (position == 0) {
            // Define tree of game states (fill GameStates array)
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

        // Perform Monte Carlo simulations

        // Initialize variables required for simulations
        double highestExpectedScore = 100;
        int highestExpectedScorePosition = 0;
        final double SIMULATIONS = 100;
        int simulationMultiplier = 0;
        double sum = 0;
        int turn = 0;
        // Variable used to determine if we should perform more simulations (continue)
        boolean cont = true;
        // Variabes used to detemrine time taken for set of simulations
        long simulationsStartTime;
        long simulationsStopTime;
        long duration;
        long currentTime;

        while (cont) {
            cont = false;
            simulationsStartTime = System.nanoTime();
            simulationMultiplier++;

            // Perform simulations for every GameState node in tree
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
                            currentScore = -1;
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

                        // Choose a random move
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
            if ((9000 - currentTime) > duration) {
                cont = true;
            }
        }

        // Monte Carlo simulations completed

        // Initialize array of available pieces (used to determine the Minimax value of each MAX GameState node)
        double availablePieces[] = new double[32];
        for (int i = 0; i < 32; i++){
            availablePieces[i] = 11;
        }

        // Determine the expected score for every GameState node (values obtained by simulations)
        for (int i = 0; i < position; i++) {
            double result = (double)(gameStates[i].getSum() / (SIMULATIONS*simulationMultiplier));
            gameStates[i].setExpectedScore(result);
        }

        // MIN selects the move (Determine Minimax value of MIN nodes)
        for (int i = 0; i < position; i++) {
            if(availablePieces[gameStates[i].getPiece()] > gameStates[i].getExpectedScore()) {
                availablePieces[gameStates[i].getPiece()] = gameStates[i].getExpectedScore();
            }
        }

        // MAX selects the piece (Determine Minimax value of MAX nodes)
        double largestSmallestValue = availablePieces[0];
        int largestSmallestValueLocation = 0;
        for (int i = 1; i < 32; i++){
            if(largestSmallestValue > 10) {
                largestSmallestValue = availablePieces[i];
                largestSmallestValueLocation = i;
            } else if (availablePieces[i] > largestSmallestValue && availablePieces[i] < 11) {
                largestSmallestValue = availablePieces[i];
                largestSmallestValueLocation = i;
            }
        }

        // Uncomment next line to print number of simulations performed
        // System.out.println(" --- Completed " + SIMULATIONS*simulationMultiplier + " simulations --- ");

        String BinaryString = String.format("%5s", Integer.toBinaryString(largestSmallestValueLocation)).replace(' ', '0');  
        return BinaryString;
    }

    /*
    * Function to find the most optimal position
    * First check if any move is a winning move, if not, move on to perform Monte Carlo simulations
    * @param pieceId
    */
    @Override
    protected String moveSelectionAlgorithm(int pieceID) {
        int drawUtilityValue = -3;
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

        // Initialize array of game states representing tree of GameState nodes
        GameState gameStates[] = new GameState[800];
        int position = 0;

        // Initialize array of potentially winning pieces for the opponent
        int piecesLeftCount = 0;
        int badPiecesCount = 0;

        // Define tree of game states (fill GameStates array)
        for (int row = 0; row < this.quartoBoard.getNumberOfRows(); row++) {
            for (int col = 0; col < this.quartoBoard.getNumberOfColumns(); col++) {
                if (!this.quartoBoard.isSpaceTaken(row, col)) {
                    // MAX chooses a move
                    QuartoBoard copyBoard = new QuartoBoard(this.quartoBoard);
                    copyBoard.insertPieceOnBoard(row, col, pieceID);

                    // Check if all pieces left will be potentially winning pieces for the opponent after MAX selects move
                    // Once piece placed on board, check remaining pieces
                    if (drawUtilityValue == -3) {
                        piecesLeftCount = 0;
                        badPiecesCount = 0;
                        // Determine the potentially winning pieces for the opponent (bad pieces)
                        boolean skip = false;
                        for (int i = 0; i < copyBoard.getNumberOfPieces(); i++) {
                            skip = false;
                            if (!copyBoard.isPieceOnBoard(i)) {
                                piecesLeftCount++;
                                for (int r = 0; r < copyBoard.getNumberOfRows(); r++) {
                                    for (int c = 0; c < copyBoard.getNumberOfColumns(); c++) {
                                        if (!copyBoard.isSpaceTaken(r, c)) {
                                            QuartoBoard copyCopyBoard = new QuartoBoard(copyBoard);
                                            copyCopyBoard.insertPieceOnBoard(r, c, i);
                                            if (copyCopyBoard.checkRow(r) || copyCopyBoard.checkColumn(c) || copyCopyBoard.checkDiagonals()) {
                                                badPiecesCount++;
                                                skip = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (skip) {
                                        break;
                                    }
                                }
                            }
                        }

                        if (piecesLeftCount == badPiecesCount) {
                            // All pieces left after MAX selects move are potentially winning pieces for opponent
                            // This is our last chance at a draw
                            // Set utility of draw to 20 to play defensively and guarantee to block opponent
                            drawUtilityValue = 20;
                        }
                    }

                    // MAX chooses a piece
                    for (int pieceNum = 0; pieceNum < copyBoard.getNumberOfPieces(); pieceNum++) {
                        if (!copyBoard.isPieceOnBoard(pieceNum)) {
                            gameStates[position] = new GameState(1, copyBoard);
                            gameStates[position].setMove(row, col);
                            gameStates[position].setPiece(pieceNum);
                            position++;
                        }
                    }
                }
            }
        }

        // Perform Monte Carlo simulations

        // Initialize variables required for simulations
        final double MAX_SIMULATIONS = 100;
        double sum = 0;
        int simulationMultiplierRound1 = 0;
        int simulationMultiplierRound2 = 0;
        // Variable used to determine if we should perform more simulations (continue)
        boolean cont = true;
        double totalSum = 0;
        double averageSum = 0;

        // Round 1:
        // Monte Carlo simulations for all GameState nodes in tree
        // Sets of 100 simulations performed until 6 second mark

        cont = true;
        // Variabes used to detemrine time taken for set of simulations
        long simulationsStartTime;
        long simulationsStopTime;
        long duration;
        long currentTime;
        int stateCounter = 0;

        while (cont) {
            cont = false;
            simulationsStartTime = System.nanoTime();
            simulationMultiplierRound1++;

            // Perform simulations for every GameState node in tree
            for (int i = 0; i < position; i++) {
                int turn = 0;
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
                            currentScore = drawUtilityValue;
                            break;
                        }

                        // Switch turns
                        if(turn == 1) {
                            turn = 0;
                        } else {
                            turn = 1;
                        }

                        // Choose a random move
                        randomMove = currentBoard.chooseRandomPositionNotPlayed(100);
                        currentBoard.insertPieceOnBoard(randomMove[0], randomMove[1], randomPieceID);

                        // Choose a random piece
                        randomPieceID = currentBoard.chooseRandomPieceNotPlayed(100);
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
            if ((6000 - currentTime) > duration) {
                cont = true;
            }
        }

        // Calculate the average simulation sum for all GameState nodes in tree
        averageSum = (double) totalSum / stateCounter;

        // Round 2:
        // Monte Carlo simulations for all GameState nodes in tree that have a simulation sum that is above average
        // Sets of 100 simulations until 9 second mark

        cont = true;
        totalSum = 0;
        stateCounter = 0;

        // Determine which GameState nodes are above average
        for (int i = 0; i < position; i++) {
            if(gameStates[i].getSum() >= averageSum) {
                gameStates[i].setAboveAverage(true);
            }
        }

        while (cont) {
            cont = false;
            simulationsStartTime = System.nanoTime();
            simulationMultiplierRound2++;

            // Perform simulations for every GameState node in tree that have a simulation sum that is above average
            for (int i = 0; i < position; i++) {
                if(gameStates[i].getAboveAvearge()) {
                    int turn = 0;
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
                                currentScore = drawUtilityValue;
                                break;
                            }

                            // Switch turns
                            if(turn == 1) {
                                turn = 0;
                            } else {
                                turn = 1;
                            }

                            // Choose a random move
                            randomMove = currentBoard.chooseRandomPositionNotPlayed(100);
                            currentBoard.insertPieceOnBoard(randomMove[0], randomMove[1], randomPieceID);

                            // Choose a random piece
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
            if ((9000 - currentTime) > duration) {
                cont = true;
            }
        }

        // Calculate the average simulation sum for all GameState nodes included in Round 2
        averageSum = (double) totalSum / stateCounter;

        // Determine the highest Minimax value for MAX nodes in GameState tree
        double highestExpectedScore = -100;
        int highestExpectedScorePosition = -1;
        for (int i = 0; i < position; i++) {
            if(gameStates[i].getAboveAvearge()) {
                double result = (double)(gameStates[i].getSum() / (MAX_SIMULATIONS*simulationMultiplierRound2 + MAX_SIMULATIONS*simulationMultiplierRound1));
                gameStates[i].setExpectedScore(result);
                if (result > highestExpectedScore) {
                    highestExpectedScore = result;
                    highestExpectedScorePosition = i;
                }
            }
        }

        // Uncomment next line to print number of simulations performed
        // System.out.println(" --- Completed " + (MAX_SIMULATIONS*simulationMultiplierRound2 + MAX_SIMULATIONS*simulationMultiplierRound1) + " simulations --- ");

        return gameStates[highestExpectedScorePosition].getMove();
    }

    // loop through board and see if the game is in a won state
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

    // loop through board and see if the game is a draw
    private boolean checkIfGameIsDraw(QuartoBoard board) {
        return board.checkIfBoardIsFull();
    }
}
