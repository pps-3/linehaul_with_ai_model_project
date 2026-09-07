
const OFFLINE = "Sorry, I couldn't connect to the Linehaul server. Please try again."
const DATA_PROBLEM = "Sorry, I couldn't retrieve the current Linehaul data. Please try again."

export async function askAssistant(message) {
  let response

  try {
    response = await fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message }),
    })
  } catch {
    return OFFLINE
  }

  if (!response.ok) {
    return DATA_PROBLEM
  }

  try {
    const data = await response.json()
    return data?.answer || DATA_PROBLEM
  } catch {
    return DATA_PROBLEM
  }
}

export default askAssistant
