# Student Management System

A Java console application to add, view, search, update, and delete students. Student records are saved in `students.csv` in the directory where the application is run.

The repository also includes a browser-based version of the student manager. When GitHub Pages is enabled for the repository, the included workflow deploys it automatically. The website stores records in the browser's local storage; records are not uploaded or synchronized between browsers.

## Requirements

- Java 8 or later

## Compile and run

Open PowerShell in this project folder and run:

```powershell
javac StudentManagementSystem.java
java StudentManagementSystem
```

The CSV file is created automatically after the first successful change. Keep `students.csv` to retain your data between runs.

## GitHub and live hosting

The Java console application itself runs in a terminal and cannot be hosted as a website. Open `index.html` in a browser to use the included web version locally. To publish it, push the project to a GitHub repository with the `main` branch, then enable GitHub Pages in the repository settings and select GitHub Actions as the build and deployment source. The workflow in `.github/workflows/pages.yml` publishes the browser version.

The local `students.csv` file and compiled `.class` files are excluded from Git so private records and build output are not published.
