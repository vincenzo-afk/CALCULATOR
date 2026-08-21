# Contributing to MathX

Thank you for your interest in contributing to MathX! We welcome contributions from the community to help make this calculator the best it can be.

## How to Contribute

1.  **Fork the Repository**: Create a fork of this repository on your own GitHub account.
2.  **Clone the Fork**: Clone your fork to your local machine.
    ```bash
    git clone https://github.com/YOUR_USERNAME/CALCULATOR.git
    ```
3.  **Create a Branch**: Create a new branch for your feature or bug fix.
    ```bash
    git checkout -b feature/your-feature-name
    ```
4.  **Make Changes**: Implement your changes, ensuring they follow the project's coding style and conventions.
5.  **Run Tests**: Verify your changes by running the existing test suite.
    ```bash
    ./gradlew test
    ```
6.  **Commit Changes**: Commit your changes with a clear and descriptive commit message.
    ```bash
    git commit -m "feat: add support for graphing mode"
    ```
7.  **Push to GitHub**: Push your branch to your fork on GitHub.
    ```bash
    git push origin feature/your-feature-name
    ```
8.  **Submit a Pull Request**: Open a pull request from your branch to the `main` branch of the original repository.

## Coding Standards

- Follow Kotlin coding conventions.
- Ensure all new features are accompanied by appropriate unit tests.
- Maintain the dark xCurrency-style UI consistency.
- **Security**: Never use unsafe functions like `eval()`. All mathematical expressions must be handled by the safe `ExpressionEvaluator`.

## Reporting Issues

If you find a bug or have a feature request, please open an issue on the [GitHub Issue Tracker](https://github.com/vincenzo-afk/CALCULATOR/issues). Provide as much detail as possible, including steps to reproduce the bug.

## License

By contributing to MathX, you agree that your contributions will be licensed under the project's [MIT License](LICENSE).
