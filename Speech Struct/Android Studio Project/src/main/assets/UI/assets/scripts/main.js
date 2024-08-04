document.addEventListener("DOMContentLoaded", function () {
    const menuItems = document.querySelectorAll("#menuBar .menu-item");

    menuItems.forEach((item, index) => {
        item.addEventListener("click", function () {
            menuItems.forEach(i => i.classList.remove("active"));

            item.classList.add("active");

            showSection(index);
        });
    });

    showSection(0);
});

function showSection(index) {
    const adminSections = document.querySelectorAll('.adminSection');

    adminSections.forEach(i => i.classList.remove("active"));

    adminSections[index].classList.add('active');
}