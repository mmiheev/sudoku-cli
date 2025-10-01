import java.io.File
import java.io.IOException
import kotlin.random.Random

/**
 * Main entry point for the Sudoku application.
 * Launches the game manager and displays the main menu.
 */
fun main() {
    GameManager().showMainMenu()
}

/**
 * Core Sudoku game logic implementation.
 *
 * Handles:
 * - Puzzle generation and solving
 * - Move validation and game state management
 * - Save/load functionality
 *
 * @property board current game state grid
 * @property solution complete solution for the current puzzle
 * @property initial mask of initially generated numbers (immutable)
 * @property difficulty number of hidden cells (30-60)
 */
class Sudoku {
    private val board = Array(9) { IntArray(9) }
    private val solution = Array(9) { IntArray(9) }
    private val initial = Array(9) { BooleanArray(9) }
    private var difficulty = 40

    /**
     * Generates a new Sudoku puzzle with specified difficulty.
     *
     * Algorithm:
     * 1. Fill diagonal 3x3 boxes with random numbers
     * 2. Solve the complete puzzle using backtracking
     * 3. Store the complete solution
     * 4. Remove numbers based on difficulty level
     *
     * @param difficultyLevel number of cells to empty (default: 40)
     */
    fun generateBoard(difficultyLevel: Int = 40) {
        this.difficulty = difficultyLevel

        // Initialize empty board
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                board[i][j] = 0
            }
        }

        // Fill diagonal 3x3 boxes with valid Sudoku numbers
        for (i in 0 until 9 step 3) {
            fillBox(i, i)
        }

        // Solve the puzzle to get complete grid
        solve()

        // Store the complete solution
        for (i in 0 until 9) {
            solution[i] = board[i].copyOf()
        }

        // Remove numbers to create the puzzle
        removeNumbers(difficultyLevel)

        // Mark initial numbers (cannot be modified by player)
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                initial[i][j] = board[i][j] != 0
            }
        }
    }

    /**
     * Fills a 3x3 box with shuffled numbers 1-9.
     *
     * @param row starting row index of the box (0, 3, or 6)
     * @param col starting column index of the box (0, 3, or 6)
     */
    private fun fillBox(row: Int, col: Int) {
        val numbers = (1..9).shuffled()
        var index = 0
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                board[row + i][col + j] = numbers[index++]
            }
        }
    }

    /**
     * Solves the Sudoku puzzle using recursive backtracking algorithm.
     *
     * @return true if puzzle is solvable, false otherwise
     */
    private fun solve(): Boolean {
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                if (board[i][j] == 0) {
                    for (num in 1..9) {
                        if (isValid(i, j, num)) {
                            board[i][j] = num
                            if (solve()) return true
                            board[i][j] = 0 // Backtrack
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    /**
     * Validates if a number can be placed at specified position.
     *
     * @param row row index to validate (0-8)
     * @param col column index to validate (0-8)
     * @param num number to validate (1-9)
     * @return true if placement is valid, false otherwise
     */
    private fun isValid(row: Int, col: Int, num: Int): Boolean {
        // Check row and column
        for (i in 0 until 9) {
            if (board[row][i] == num || board[i][col] == num) return false
        }

        // Check 3x3 box
        val startRow = row - row % 3
        val startCol = col - col % 3
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                if (board[startRow + i][startCol + j] == num) return false
            }
        }
        return true
    }

    /**
     * Removes numbers from complete grid to create puzzle.
     *
     * @param difficulty number of cells to empty
     */
    private fun removeNumbers(difficulty: Int) {
        var count = difficulty
        while (count != 0) {
            val cellId = Random.nextInt(81)
            val i = cellId / 9
            val j = cellId % 9

            if (board[i][j] != 0) {
                board[i][j] = 0
                count--
            }
        }
    }

    /**
     * Prints the current game board to console with formatting.
     *
     * Color coding:
     * - Blue: initial puzzle numbers (immutable)
     * - Green: player-input numbers
     * - Dots: empty cells
     */
    fun printBoard() {
        println("\n  A B C | D E F | G H I")
        for (i in 0 until 9) {
            print("${i + 1} ")
            for (j in 0 until 9) {
                when {
                    board[i][j] == 0 -> print("·")
                    initial[i][j] -> print("\u001B[34m${board[i][j]}\u001B[0m") // Blue
                    else -> print("\u001B[32m${board[i][j]}\u001B[0m") // Green
                }
                print(" ")
                if (j == 2 || j == 5) print("| ")
            }
            println()
            if (i == 2 || i == 5) {
                println("  ------+-------+------")
            }
        }
    }

    /**
     * Executes a player move on the game board.
     *
     * @param row row index (0-8)
     * @param col column index (0-8)
     * @param number number to place (1-9)
     * @return true if move was valid and executed, false otherwise
     */
    fun makeMove(row: Int, col: Int, number: Int): Boolean {
        if (row !in 0..8 || col !in 0..8 || number !in 1..9) return false
        if (initial[row][col]) return false // Cannot modify initial numbers

        board[row][col] = number
        return true
    }

    /**
     * Checks if current board state matches the solution.
     *
     * @return true if puzzle is solved correctly, false otherwise
     */
    fun checkSolution(): Boolean {
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                if (board[i][j] != solution[i][j]) return false
            }
        }
        return true
    }

    // Public getters for game state access
    fun getCell(row: Int, col: Int): Int = board[row][col]
    fun getSolution(): Array<IntArray> = solution
    fun getDifficulty(): Int = difficulty

    /**
     * Saves current game state to file.
     *
     * File format:
     * - Line 1: difficulty level
     * - Lines 2-10: current board state (space-separated numbers)
     * - Lines 11-19: initial number mask (1 for initial, 0 for player input)
     *
     * @param filename output filename (default: "sudoku_save.txt")
     * @return true if save successful, false on error
     */
    fun saveGame(filename: String = "sudoku_save.txt"): Boolean {
        return try {
            val writer = File(filename).bufferedWriter()
            writer.write("$difficulty\n")

            // Save current board state
            for (i in 0 until 9) {
                for (j in 0 until 9) {
                    writer.write("${board[i][j]} ")
                }
                writer.write("\n")
            }

            // Save initial number mask
            for (i in 0 until 9) {
                for (j in 0 until 9) {
                    writer.write(if (initial[i][j]) "1" else "0")
                }
                writer.write("\n")
            }
            writer.close()
            true
        } catch (_: IOException) {
            false
        }
    }

    /**
     * Loads game state from file.
     *
     * @param filename input filename (default: "sudoku_save.txt")
     * @return true if load successful, false on error
     */
    fun loadGame(filename: String = "sudoku_save.txt"): Boolean {
        return try {
            val lines = File(filename).readLines()
            if (lines.size < 19) return false

            difficulty = lines[0].toInt()

            // Load board state
            for (i in 0 until 9) {
                val numbers = lines[i + 1].trim().split(" ")
                for (j in 0 until 9) {
                    board[i][j] = numbers[j].toInt()
                }
            }

            // Load initial number mask
            for (i in 0 until 9) {
                val line = lines[i + 10].trim()
                for (j in 0 until 9) {
                    initial[i][j] = line[j] == '1'
                }
            }

            // Regenerate solution for loaded puzzle
            generateSolutionFromCurrent()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Generates solution for current puzzle state.
     * Used when loading saved games to reconstruct the solution.
     */
    private fun generateSolutionFromCurrent() {
        val tempBoard = Array(9) { i -> board[i].copyOf() }

        solveTemp(tempBoard)

        for (i in 0 until 9) {
            solution[i] = tempBoard[i].copyOf()
        }
    }

    /**
     * Solves puzzle on temporary board using backtracking.
     *
     * @param tempBoard temporary board array to solve
     * @return true if solution found, false otherwise
     */
    private fun solveTemp(tempBoard: Array<IntArray>): Boolean {
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                if (tempBoard[i][j] == 0) {
                    for (num in 1..9) {
                        if (isValidTemp(tempBoard, i, j, num)) {
                            tempBoard[i][j] = num
                            if (solveTemp(tempBoard)) return true
                            tempBoard[i][j] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    /**
     * Validates number placement on temporary board.
     *
     * @param tempBoard temporary board to validate against
     * @param row row index to check
     * @param col column index to check
     * @param num number to validate
     * @return true if placement is valid, false otherwise
     */
    private fun isValidTemp(tempBoard: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        for (i in 0 until 9) {
            if (tempBoard[row][i] == num || tempBoard[i][col] == num) return false
        }

        val startRow = row - row % 3
        val startCol = col - col % 3
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                if (tempBoard[startRow + i][startCol + j] == num) return false
            }
        }
        return true
    }
}

/**
 * Manages game flow and user interface.
 *
 * Handles:
 * - Menu navigation and user input
 * - Game state transitions
 * - Player interaction and commands
 */
class GameManager {
    private val game = Sudoku()

    /**
     * Displays main menu and handles user selection.
     */
    fun showMainMenu() {
        while (true) {
            println("\n=== SUDOKU MAIN MENU ===")
            println("1. New game")
            println("2. Load last game")
            println("0. Exit")
            print("Select an action: ")

            when (readlnOrNull()?.trim()) {
                "1" -> showDifficultyMenu()
                "2" -> loadGame()
                "0" -> {
                    println("Goodbye!")
                    return
                }
                else -> println("Invalid input! Please try again.")
            }
        }
    }

    /**
     * Displays difficulty selection menu and starts new game.
     */
    private fun showDifficultyMenu() {
        println("\n=== SELECT DIFFICULTY LEVEL ===")
        println("1. Easy (30 hidden numbers)")
        println("2. Medium (40 hidden numbers)")
        println("3. Hard (50 hidden numbers)")
        println("4. Expert (55 hidden numbers)")
        println("5. Master (60 hidden numbers)")
        print("Select level: ")

        val difficulty = when (readlnOrNull()?.trim()) {
            "1" -> 30
            "2" -> 40
            "3" -> 50
            "4" -> 55
            "5" -> 60
            else -> {
                println("Invalid input! Medium difficulty has been set.")
                40
            }
        }

        startNewGame(difficulty)
    }

    /**
     * Starts new game with specified difficulty.
     *
     * @param difficulty number of hidden cells
     */
    private fun startNewGame(difficulty: Int) {
        println("\nGenerating new game...")
        game.generateBoard(difficulty)
        playGame()
    }

    /**
     * Attempts to load last saved game.
     */
    private fun loadGame() {
        println("\nLoading last game...")
        if (game.loadGame()) {
            println("Game loaded successfully!")
            playGame()
        } else {
            println("Failed to load saved game. Please start a new game.")
        }
    }

    /**
     * Main game loop handling player interaction.
     *
     * Available commands:
     * - 'save': Save current game state
     * - 'menu': Return to main menu
     * - 'hint': Show hint for next empty cell
     * - 'exit': Quit application
     */
    private fun playGame() {
        println("\n=== SUDOKU ===")
        println("Difficulty level: ${getDifficultyName(game.getDifficulty())}")
        println("Enter your move in format: row column number (for example: 5 D 3)")
        println("Commands: 'save' - save game, 'menu' - main menu, 'hint' - hint")

        while (true) {
            game.printBoard()

            print("\nYour move: ")
            when (val input = readlnOrNull()?.trim()?.lowercase()) {
                "menu" -> {
                    println("Returning to the main menu...")
                    return
                }
                "save" -> {
                    if (game.saveGame()) {
                        println("Game saved successfully!")
                    } else {
                        println("Error saving the game!")
                    }
                }
                "hint" -> showHint()
                "exit" -> {
                    println("Goodbye!")
                    return
                }
                else -> {
                    if (input != null && processMove(input)) {
                        if (game.checkSolution()) {
                            game.printBoard()
                            println("\nCongratulations! You solved the sudoku!")
                            println("Press Enter to return to the main menu...")
                            readlnOrNull()
                            return
                        }
                    }
                }
            }
        }
    }

    /**
     * Processes player move input.
     *
     * Expected format: "row column number" (e.g., "5 D 3")
     *
     * @param input raw user input string
     * @return true if move was processed successfully, false on error
     */
    private fun processMove(input: String): Boolean {
        val parts = input.split(" ")
        if (parts.size != 3) {
            println("Invalid input format! Example: 5 D 3")
            return false
        }

        try {
            val row = parts[0].toInt() - 1
            val col = parts[1].uppercase()[0] - 'A'
            val number = parts[2].toInt()

            if (game.makeMove(row, col, number)) {
                return true
            } else {
                println("Invalid move! Please check your input.")
                return false
            }
        } catch (e: Exception) {
            println("Input error: ${e.message}")
            return false
        }
    }

    /**
     * Shows hint by revealing the correct number for first empty cell.
     */
    private fun showHint() {
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                if (game.getCell(i, j) == 0) {
                    val solution = game.getSolution()
                    println("Hint: cell ${i + 1} ${'A' + j} should contain number ${solution[i][j]}")
                    return
                }
            }
        }
        println("All cells are already filled!")
    }

    /**
     * Converts numeric difficulty to descriptive name.
     *
     * @param difficulty numeric difficulty level
     * @return descriptive difficulty name
     */
    private fun getDifficultyName(difficulty: Int): String {
        return when (difficulty) {
            30 -> "Easy"
            40 -> "Medium"
            50 -> "Hard"
            55 -> "Expert"
            60 -> "Master"
            else -> "Unknown"
        }
    }
}