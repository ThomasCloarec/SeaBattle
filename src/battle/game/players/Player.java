package battle.game.players;

import battle.BattleShip;
import battle.game.ShotResult;
import battle.game.ships.Ship;
import battle.text.AppText;
import view.GridTableFrame;

import java.util.ArrayList;
import java.util.List;

/**
 * This abstract class contains everything common to all players.
 */
public abstract class Player {
    /**
     * Number of players frames, this is used to not superpose them
     */
    private static int playersFramesDisplayed;
    /**
     * The fleet of the player
     */
    protected List<Ship> fleet = new ArrayList<>();
    /**
     * The height of the player's grid
     */
    protected int height;
    /**
     * The grid of the player
     */
    protected Square[][] myGrid;
    /**
     * The name of the player
     */
    protected String name;
    /**
     * The grid of the opponent of the player
     */
    protected Square[][] opponentGrid;
    /**
     * The width of the player's grid
     */
    protected int width;
    /**
     * The view of my grid
     */
    private GridTableFrame myFrame;
    /**
     * The view of the opponent's grid
     */
    private GridTableFrame opponentFrame;

    /**
     * The constructor of the Player class, it copies the fleet passed as parameter and set others attributes.
     *
     * @param fleet  the fleet to copy
     * @param name   the name of the player
     * @param width  The width of the player's grid
     * @param height The height of the player's grid
     */
    public Player(ArrayList<Ship> fleet, String name, int width, int height) {
        // Check parameters
        if (fleet == null || name == null) {
            throw new IllegalArgumentException("One or more parameter is null. See the concerned method.");
        } else if (width < BattleShip.BOARD_MIN_WIDTH || width > BattleShip.BOARD_MAX_WIDTH) {
            throw new IllegalArgumentException("The width parameter should be a number between " + BattleShip.BOARD_MIN_WIDTH + " and " + BattleShip.BOARD_MAX_WIDTH + ".");
        } else if (height < BattleShip.BOARD_MIN_HEIGHT || height > BattleShip.BOARD_MAX_HEIGHT) {
            throw new IllegalArgumentException("The height parameter should be a number between " + BattleShip.BOARD_MIN_HEIGHT + " and " + BattleShip.BOARD_MAX_HEIGHT + ".");
        }

        this.createCopy(fleet);
        this.name = name;
        this.width = width;
        this.height = height;

        this.initializeMyGrid();
        this.initializeOpponentGrid();
    }

    /**
     * Get a square on a grid (check its validity)
     *
     * @param grid   the grid
     * @param line   the line of the square
     * @param column the column of the square
     * @return the square
     */
    protected static Square getSquareOnGrid(Square[][] grid, int line, int column) {
        // Check parameters
        if (grid == null) {
            throw new IllegalArgumentException("One or more parameter is null. See the concerned method.");
        }

        Square square = null;

        if (line >= 0 && line < grid.length) {
            if (column >= 0 && column < grid[0].length) {
                square = grid[line][column];
            }
        }
        return square;
    }

    /**
     * Increment the number of players frames
     */
    private static void incrementPlayersFramesCount() {
        Player.playersFramesDisplayed++;
    }

    /**
     * Decrement the number of players frames
     */
    private static void decrementPlayersFramesCount() {
        Player.playersFramesDisplayed--;
    }

    /**
     * Initialize a grid with squares
     *
     * @param grid the grid to initialize
     */
    private static void initializeGrid(Square[][] grid) {
        // Check parameters
        if (grid == null) {
            throw new IllegalArgumentException("One or more parameter is null. See the concerned method.");
        }

        for (int line = 0; line < grid.length; line++) {
            for (int column = 0; column < grid[0].length; column++) {
                grid[line][column] = new Square(line, column);
            }
        }
    }

    /**
     * Ask two positions to the player for where to shoot
     *
     * @return the two positions
     */
    public abstract int[] newShot();

    /**
     * Initialize positions of ships in fleet
     */
    public abstract void shipPlacement();

    /**
     * Initialize grids.
     */
    public void initializeGrids() {
        Player.initializeGrid(this.myGrid);
        this.shipPlacement();
        Player.initializeGrid(this.opponentGrid);
    }

    /**
     * Display the grid of the actual player
     */
    public void displayMygrid() {
        Player.incrementPlayersFramesCount();
        this.myFrame = new GridTableFrame(this.myGrid);
        this.myFrame.showIt("[" + this.name + "] " + AppText.getTextFor("my_grid"), 500 * (Player.playersFramesDisplayed - 1), 0);
    }

    /**
     * Close grids to save calculation performance (used in AA mode)
     */
    public void closeGrids() {
        if (this.myFrame != null) {
            this.myFrame.dispose();
            this.myFrame = null;
        }
        if (this.opponentFrame != null) {
            this.opponentFrame.dispose();
            this.opponentFrame = null;
        }
        Player.decrementPlayersFramesCount();
    }

    /**
     * Analyse the shot sent by the opponent and tell if it is a MISS, an HIT or a SUNK
     *
     * @param shot the shot position
     * @return the ShotResult
     */
    public ShotResult analyzeShot(int[] shot) {
        // Check parameters
        if (shot == null) {
            throw new IllegalArgumentException("One or more parameter is null. See the concerned method.");
        }

        ShotResult shotResult = ShotResult.MISS;

        int line = shot[0];
        int column = shot[1];

        if (!this.myGrid[line][column].isHit() && !this.myGrid[line][column].isFree()) {
            Ship ship = this.getShipOnPosition(line, column);
            ship.addHit();
            if (ship.isSunk()) {
                shotResult = ShotResult.SUNK;
            } else {
                shotResult = ShotResult.HIT;
            }
        }

        this.myGrid[line][column].setHit();
        if (this.myFrame != null) {
            this.myFrame.repaint();
        }

        return shotResult;
    }

    /**
     * Display the grid of the opponent
     */
    public void displayOpponentGrid() {
        this.opponentFrame = new GridTableFrame(this.opponentGrid);
        this.opponentFrame.showIt("[" + this.name + "] " + AppText.getTextFor("opponent_grid"), 500 * (Player.playersFramesDisplayed - 1), 500);
    }

    /**
     * Check if the whole fleet of the player is sunk
     *
     * @return is the fleet sunk
     */
    public boolean allSunk() {
        boolean allSunk = true;

        int i = 0;
        while (allSunk && i < this.fleet.size()) {
            allSunk = this.fleet.get(i).isSunk();
            i++;
        }

        return allSunk;
    }

    /**
     * Update the opponent grid using the result of the shot
     *
     * @param shotResult   the result of the shot
     * @param shotPosition the position of the shot
     */
    public void sendLastShotResult(ShotResult shotResult, int[] shotPosition) {
        // Check parameters
        if (shotResult == null || shotPosition == null) {
            throw new IllegalArgumentException("One or more parameter is null. See the concerned method.");
        }

        int line = shotPosition[0];
        int column = shotPosition[1];
        this.opponentGrid[line][column].setHit();
        if (shotResult == ShotResult.HIT || shotResult == ShotResult.SUNK) {
            this.opponentGrid[line][column].setBusy();
        }

        if (this.opponentFrame != null) {
            this.opponentFrame.repaint();
        }
    }

    /**
     * Check the validity of the squares passed as parameter
     *
     * @param squares the squares
     * @return true if can we insert them on the grid
     */
    protected boolean checkPosition(Square[] squares) {
        // Check parameters
        if (squares == null) {
            throw new IllegalArgumentException("One or more parameter is null. See the concerned method.");
        }

        boolean allowedPosition = true;
        int i = 0;
        while (allowedPosition && i < squares.length) {
            allowedPosition = this.isSquareAllowingPlacement(squares[i]);
            i++;
        }
        return allowedPosition;
    }

    /**
     * Create a copy of a fleet and put it into the fleet attribute of the player object
     *
     * @param fleet the fleet to copy
     */
    protected void createCopy(ArrayList<Ship> fleet) {
        // Check parameters
        if (fleet == null) {
            throw new IllegalArgumentException("One or more parameter is null. See the concerned method.");
        }

        this.fleet.clear();

        try {
            for (Ship ship : fleet) {
                this.fleet.add(ship.clone());
            }
        } catch (CloneNotSupportedException e) {
            System.err.println("Can't copy ship");
        }
    }

    /**
     * Initialize the grid of the player
     */
    protected void initializeMyGrid() {
        this.myGrid = new Square[this.width][this.height];
        Player.initializeGrid(this.myGrid);
        this.shipPlacement();
    }

    /**
     * Initialize the grid of the opponent player
     */
    protected void initializeOpponentGrid() {
        this.opponentGrid = new Square[this.width][this.height];
        Player.initializeGrid(this.opponentGrid);
    }

    /**
     * Check if a particular square allows placement
     *
     * @param square the square
     * @return the answer
     */
    protected boolean isSquareAllowingPlacement(Square square) {
        // Check parameters
        if (square == null) {
            throw new IllegalArgumentException("One or more parameter is null. See the concerned method.");
        }

        boolean ret = false;

        if (square.getLine() >= 0 && square.getLine() < this.myGrid.length) {
            if (square.getColumn() >= 0 && square.getColumn() < this.myGrid[0].length) {
                // Check if the current and all squares around are free

                ret = square.isFree();

                if (ret) {
                    Square topSquare = this.getSquareOnGrid(square.getLine() - 1, square.getColumn());
                    if (topSquare != null) {
                        ret = topSquare.isFree();
                    }
                }

                if (ret) {
                    Square bottomSquare = this.getSquareOnGrid(square.getLine() + 1, square.getColumn());
                    if (bottomSquare != null) {
                        ret = bottomSquare.isFree();
                    }
                }

                if (ret) {
                    Square leftSquare = this.getSquareOnGrid(square.getLine(), square.getColumn() - 1);
                    if (leftSquare != null) {
                        ret = leftSquare.isFree();
                    }
                }

                if (ret) {
                    Square rightSquare = this.getSquareOnGrid(square.getLine(), square.getColumn() + 1);
                    if (rightSquare != null) {
                        ret = rightSquare.isFree();
                    }
                }

                if (ret) {
                    Square topRightSquare = this.getSquareOnGrid(square.getLine() - 1, square.getColumn() + 1);
                    if (topRightSquare != null) {
                        ret = topRightSquare.isFree();
                    }
                }

                if (ret) {
                    Square topLeftSquare = this.getSquareOnGrid(square.getLine() - 1, square.getColumn() - 1);
                    if (topLeftSquare != null) {
                        ret = topLeftSquare.isFree();
                    }
                }

                if (ret) {
                    Square bottomRightSquare = this.getSquareOnGrid(square.getLine() + 1, square.getColumn() + 1);
                    if (bottomRightSquare != null) {
                        ret = bottomRightSquare.isFree();
                    }
                }

                if (ret) {
                    Square bottomLeftSquare = this.getSquareOnGrid(square.getLine() + 1, square.getColumn() - 1);
                    if (bottomLeftSquare != null) {
                        ret = bottomLeftSquare.isFree();
                    }
                }
            }
        }

        return ret;
    }

    /**
     * Get a square on my grid (check its validity)
     *
     * @param line   the line of the square
     * @param column the column of the square
     * @return the square
     */
    private Square getSquareOnGrid(int line, int column) {
        return Player.getSquareOnGrid(this.myGrid, line, column);
    }

    /**
     * Check if a particular ship of the player is sunk
     *
     * @param line   the line of the ship
     * @param column the column of the ship
     * @return is the ship sunk (return false if there is no ship there)
     */
    private boolean isSunk(int line, int column) {
        boolean sunk = false;

        Ship ship = this.getShipOnPosition(line, column);
        if (ship != null) {
            sunk = ship.isSunk();
        }

        return sunk;
    }

    /**
     * get the ship placed on a particular position
     *
     * @param line   the line of the ship
     * @param column the column of the ship
     * @return the ship (return null if there is none)
     */
    private Ship getShipOnPosition(int line, int column) {
        Ship ship = null;

        int i = 0;
        while (ship == null && i < this.fleet.size()) {
            Ship testShip = this.fleet.get(i);
            if (testShip.contains(line, column)) {
                ship = testShip;
            }
            i++;
        }

        return ship;
    }

    /**
     * Get the number of hits received across all ships
     *
     * @return the number of hits
     */
    public int getAllHitNumber() {
        int value = 0;
        for (Ship ship : this.fleet) {
            value += ship.getHitNumber();
        }
        return value;
    }

    /**
     * Gets The name of the player.
     *
     * @return Value of The name of the player.
     */
    public String getName() {
        return this.name;
    }
}
