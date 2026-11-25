/** Validación común de formularios: campos obligatorios sin espacios,
 * reglas HTML nativas (tipo, longitud y rangos) y confirmación de campos. */
export function validateFormFields(event) {
  const form = event.target;
  if (!(form instanceof HTMLFormElement)) return;

  const trimTypes = new Set(["text", "search", "email", "tel", "url", "password"]);
  for (const field of Array.from(form.elements)) {
    if (!(field instanceof HTMLElement) || !("willValidate" in field) || !field.willValidate) continue;
    field.setCustomValidity("");
    const type = String(field.type || "").toLowerCase();
    const value = String(field.value ?? "");
    if (field.required && (trimTypes.has(type) || field instanceof HTMLTextAreaElement) && value.trim() === "") {
      field.setCustomValidity("Este campo es obligatorio y no puede contener solo espacios.");
    }
  }

  const matchFields = String(form.dataset.matchFields || "").split(",").map((value) => value.trim()).filter(Boolean);
  if (matchFields.length === 2) {
    const [sourceId, confirmationId] = matchFields;
    const source = Array.from(form.elements).find((field) => field.id === sourceId);
    const confirmation = Array.from(form.elements).find((field) => field.id === confirmationId);
    if (source && confirmation) {
      confirmation.setCustomValidity(
        String(source.value ?? "") === String(confirmation.value ?? "")
          ? ""
          : "Los valores de confirmación no coinciden."
      );
    }
  }

  if (!form.checkValidity()) {
    event.preventDefault();
    event.stopPropagation();
    form.reportValidity();
  }
}
