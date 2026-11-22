"use strict";

const STORAGE_KEY = "student-management-records-v1";
const studentList = document.querySelector("#student-list");
const searchInput = document.querySelector("#search-input");
const countElement = document.querySelector("#student-count");
const emptyState = document.querySelector("#empty-state");
const emptyTitle = document.querySelector("#empty-title");
const emptyMessage = document.querySelector("#empty-message");
const dialog = document.querySelector("#student-dialog");
const form = document.querySelector("#student-form");
const formError = document.querySelector("#form-error");
const dialogTitle = document.querySelector("#dialog-title");
const saveButton = document.querySelector("#save-button");
const toast = document.querySelector("#toast");

let students = loadStudents();
let editingId = null;
let toastTimer;

function loadStudents() {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
    if (!Array.isArray(saved)) throw new Error("Saved data is not a list.");
    return saved.filter((student) =>
      student
      && typeof student.id === "string"
      && typeof student.name === "string"
      && typeof student.branch === "string"
    );
  } catch (error) {
    console.error("Could not load student records:", error);
    showToast("Could not load saved records.");
    return [];
  }
}

function saveStudents() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(students));
    return true;
  } catch (error) {
    console.error("Could not save student records:", error);
    showToast("Could not save records in this browser.");
    return false;
  }
}

function renderStudents() {
  const query = searchInput.value.trim().toLocaleLowerCase();
  const visibleStudents = students.filter((student) =>
    student.id.toLocaleLowerCase().includes(query)
    || student.name.toLocaleLowerCase().includes(query)
    || student.branch.toLocaleLowerCase().includes(query)
  );

  countElement.textContent = String(students.length);
  studentList.replaceChildren();

  for (const student of visibleStudents) {
    const row = document.createElement("tr");
    const studentCell = document.createElement("td");
    const identity = document.createElement("div");
    const avatar = document.createElement("span");
    const name = document.createElement("span");
    const idCell = document.createElement("td");
    const branchCell = document.createElement("td");
    const actionsCell = document.createElement("td");
    const actions = document.createElement("div");
    const editButton = document.createElement("button");
    const deleteButton = document.createElement("button");

    avatar.className = "avatar";
    avatar.setAttribute("aria-hidden", "true");
    avatar.textContent = student.name.trim().charAt(0).toLocaleUpperCase();
    identity.className = "student-cell";
    name.textContent = student.name;
    identity.append(avatar, name);
    studentCell.append(identity);
    idCell.textContent = student.id;
    const branch = document.createElement("span");
    branch.className = "branch-pill";
    branch.textContent = student.branch;
    branchCell.append(branch);

    actions.className = "row-actions";
    editButton.type = "button";
    editButton.className = "action-button";
    editButton.textContent = "Edit";
    editButton.setAttribute("aria-label", `Edit ${student.name}`);
    editButton.addEventListener("click", () => openDialog(student));
    deleteButton.type = "button";
    deleteButton.className = "action-button action-delete";
    deleteButton.textContent = "Delete";
    deleteButton.setAttribute("aria-label", `Delete ${student.name}`);
    deleteButton.addEventListener("click", () => deleteStudent(student.id));
    actions.append(editButton, deleteButton);
    actionsCell.append(actions);
    row.append(studentCell, idCell, branchCell, actionsCell);
    studentList.append(row);
  }

  emptyState.hidden = visibleStudents.length > 0;
  if (students.length > 0 && visibleStudents.length === 0) {
    emptyTitle.textContent = "No matching students";
    emptyMessage.textContent = "Try another name, ID, or branch.";
  } else {
    emptyTitle.textContent = "No students yet";
    emptyMessage.textContent = "Add a student to get started.";
  }
}

function openDialog(student = null) {
  editingId = student ? student.id : null;
  form.reset();
  formError.textContent = "";
  dialogTitle.textContent = student ? "Edit student" : "Add student";
  saveButton.textContent = student ? "Save changes" : "Add student";
  document.querySelector("#student-id").disabled = Boolean(student);
  if (student) {
    form.elements.id.value = student.id;
    form.elements.name.value = student.name;
    form.elements.branch.value = student.branch;
  }
  dialog.showModal();
  document.querySelector("#student-id").focus();
}

function closeDialog() {
  dialog.close();
  formError.textContent = "";
}

function submitStudent(event) {
  event.preventDefault();
  const id = form.elements.id.value.trim();
  const name = form.elements.name.value.trim();
  const branch = form.elements.branch.value.trim();

  if (!id || !name || !branch) {
    formError.textContent = "Please fill in all fields.";
    return;
  }
  if (!editingId && students.some((student) => student.id.toLocaleLowerCase() === id.toLocaleLowerCase())) {
    formError.textContent = "A student with this ID already exists.";
    return;
  }

  const previousStudents = students;
  if (editingId) {
    students = students.map((student) => student.id === editingId ? { id, name, branch } : student);
  } else {
    students = [...students, { id, name, branch }];
  }

  if (!saveStudents()) {
    students = previousStudents;
    return;
  }

  closeDialog();
  renderStudents();
  showToast(editingId ? "Student updated." : "Student added.");
}

function deleteStudent(id) {
  const student = students.find((record) => record.id === id);
  if (!student || !window.confirm(`Delete ${student.name} (ID ${student.id})?`)) return;

  const previousStudents = students;
  students = students.filter((record) => record.id !== id);
  if (!saveStudents()) {
    students = previousStudents;
    return;
  }

  renderStudents();
  showToast("Student deleted.");
}

function showToast(message) {
  toast.textContent = message;
  toast.classList.add("is-visible");
  window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => toast.classList.remove("is-visible"), 2600);
}

document.querySelector("#add-student-button").addEventListener("click", () => openDialog());
document.querySelector("#close-dialog-button").addEventListener("click", closeDialog);
document.querySelector("#cancel-button").addEventListener("click", closeDialog);
form.addEventListener("submit", submitStudent);
searchInput.addEventListener("input", renderStudents);
dialog.addEventListener("click", (event) => {
  if (event.target === dialog) closeDialog();
});

renderStudents();
